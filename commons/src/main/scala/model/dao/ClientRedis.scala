package model.dao

import java.util.AbstractMap.SimpleImmutableEntry

import io.lettuce.core.api.sync.RedisCommands
import model.logger.Log
import redis.clients.jedis.{Jedis, StreamEntry, StreamEntryID}

import scala.collection.JavaConverters._

/**
 * Create a space for executing Redis commands.
 * Give to the body a connection to the db and after all operations close the connection.
 *
 * @param body
 * Function
 */
class ClientRedis[T](body: Jedis => T) {

  def createSpace(): T = {
    val client = RedisConnection.getConnection

    try {
      body(client)
    } finally {
      client.close()
    }

  }
}

object ClientRedis {
  def apply[T](body: Jedis => T): T = {
    new ClientRedis[T](body).createSpace()
  }


  def defaultAddToMainStream(fields: Map[String, String]): Unit = {
    ClientRedis {
      client =>
        client.xadd(SENSOR_MAIN_STREAM_KEY, StreamEntryID.NEW_ENTRY, fields.asJava, STREAM_MAX_LENGTH, true)
    }
  }

  def addError(error: LogError): Unit = {
    ClientRedis {
      client =>
        client.xadd(ERROR_STREAM_KEY, StreamEntryID.NEW_ENTRY, error.toMap.asJava, STREAM_MAX_LENGTH, true)
    }
  }

  def readStreamAsGroup(stream: String, group: String, clientName: String, lastStreamID: StreamEntryID = StreamEntryID.UNRECEIVED_ENTRY): Option[(String, Seq[StreamEntry])] = {
    ClientRedis(db => {
      val streamQuery = new SimpleImmutableEntry[String, StreamEntryID](stream, lastStreamID)
      val result = db
        .xreadGroup(group, clientName, 1, 5000L, false, streamQuery)

      if (Option(result).isEmpty) return None

      result.asScala
        .find(e => e.getKey.equals(stream))
        .map(e => (e.getKey, e.getValue.asScala)) match {
        case Some(value) => if (value._2.isEmpty) return None else return Some(value)
        case None => return None
      }
    })
  }

  def sendAck(group: String, streamEntryID: StreamEntryID, stream: String = SENSOR_MAIN_STREAM_KEY): Unit = {
    ClientRedis {
      _.xack(stream, group, streamEntryID)
    }
  }

  def initializeConsumerGroup(consumerGroup: String, streamKey: String = SENSOR_MAIN_STREAM_KEY): Unit = {
    ClientRedis {
      client =>
        try {
          client.xgroupCreate(streamKey, consumerGroup, StreamEntryID.LAST_ENTRY, true)
        } catch {
          case _: Throwable => Log.info(s"$consumerGroup group already in stream")
        }
    }
  }

  def getConsumerId(groupIdKey: String) = ClientRedis { _.incr(groupIdKey) }.toString
}


/**
 * Create a space for executing Redis commands.
 * Give to the body a connection to the db and after all operations close the connection.
 *
 * @param body
 * Function
 */
class LettuceRedis[T](body: RedisCommands[String, String]=> T) {

  def createSpace(): T = {
    val client = RedisConnection.getLettuceConnection
    val connection = client.connect()

    try {
      body(connection.sync())
    } finally {
      connection.async()
      client.shutdownAsync()
    }

  }
}

object LettuceRedis {
  def apply[T](body: RedisCommands[String, String] => T): T = new LettuceRedis(body).createSpace()
}