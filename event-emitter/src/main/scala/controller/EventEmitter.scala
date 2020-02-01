package controller

import model.api.ErrorStreamEntry
import model.dao.ClientRedis.readStreamAsGroup
import model.dao.{ClientRedis, EVENT_EMITTER_GROUP, LogError, SENSOR_MAIN_STREAM_KEY, SensorRead}
import model.logger.Log
import org.json4s.jackson.Serialization.write
import redis.clients.jedis.StreamEntryID

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

case class EventEmitter(group: String, consumerId: String) {
  ClientRedis.initializeConsumerGroup(group)

  def start(): Future[Unit] = {
    Future {
      while (true) {
        try {
          readStreamAsGroup(SENSOR_MAIN_STREAM_KEY, group, consumerId) match {
            case Some(data) =>
              data._2.map(e => SensorRead(e)).foreach(onStreamEntry)
            case None =>
              Log.debug("Nessun dato da leggere ...")
          }
        } catch {
          case e: Exception => Log.error(e.getMessage)
        }

      }
    }
  }

  /**
   * Check if read is ok and send ACK
   *
   */
  private def onStreamEntry(entry: SensorRead): Unit = {
    def sendError(msg: String): Unit = {
      Log.warn(msg)

      ClientRedis.addError(LogError(msg, entry.name, zone = entry.zone))
    }

    Future {
      if (entry.temperature > 40) sendError(s"Zone ${entry.zone} hot alarm!! Check as soon as possible!!")
      if (entry.temperature < -10) sendError(s"Zone ${entry.zone} cold alarm!! Check as soon as possible!!")
      if (entry.humidity <= 0) sendError(s"Zone ${entry.zone} possibile humidity fail relevation!! Check as soon as possible!!")
    } andThen {
      case _ => ClientRedis.sendAck(EVENT_EMITTER_GROUP, new StreamEntryID(entry.id))
    }
  }

}