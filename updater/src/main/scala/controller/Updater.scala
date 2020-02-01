package controller


import java.sql.Timestamp

import com.corundumstudio.socketio.SocketIOServer
import model.api.ErrorStreamEntry
import model.dao._
import model.logger.Log
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import redis.clients.jedis.StreamEntryID

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class Updater(server: SocketIOServer, group: String, consumerId: String) {

  implicit val formats: DefaultFormats.type = DefaultFormats

  ClientRedis.initializeConsumerGroup(group)
  ClientRedis.initializeConsumerGroup(group, ERROR_STREAM_KEY)

  /**
   * Start the Updater thread that read the main and the error stream and update clients by web socket
   * @return
   */
  def start(): Future[Unit] = {
    Log.debug("Updater is running...")
    Future {
      while (true) {
        try {
          ClientRedis.readStreamAsGroup(SENSOR_MAIN_STREAM_KEY, group, consumerId) match {
            case Some(data) =>
              data._2.map(e => SensorRead(e)).foreach(onStreamEntry)
              data._2.foreach(e => ClientRedis.sendAck(group, e.getID))
            case None =>
              Log.debug(s"Nessun dato da leggere da $group...")
          }
        } catch {
          case e: Throwable => Log error e
        }

      }
    }

    Future {
      while (true) {
        try {
          ClientRedis.readStreamAsGroup(ERROR_STREAM_KEY, group, consumerId) match {
            case Some(data) =>
              data._2.map(LogError getEntryWithID).foreach(onErrorStreamEntry)
            case None =>
              Log.debug(s"Nessun dato da leggere da $group...")
          }
        } catch {
          case e: Throwable => Log error e
        }
      }
    }
  }

  /**
   * Send broadcast message by web socket on channel sensor-read-update
   *
   * @param entry
   * Data read from sensor
   */
  private def onStreamEntry(entry: SensorRead): Unit = {
    server.getBroadcastOperations.sendEvent("sensor-read-update", write(entry))
  }

  /**
   * Sens broadcast message by web socket on channel sensor-error-update
   *
   * @param entry
   * System error
   */
  private def onErrorStreamEntry(entry: (StreamEntryID, ErrorStreamEntry)): Unit = {
    server.getBroadcastOperations.sendEvent("sensor-error-update", write(entry._2))
    ClientRedis.sendAck(group, entry._1, ERROR_STREAM_KEY)
  }

}

case class SensorReadMessageUpdate(zone: String, temperature: Double, humidity: Double, dateTime: Timestamp, coordinate: Option[Coordinate])

object Updater {
  def apply(server: SocketIOServer, group: String, consumerId: String): Updater = new Updater(server, group, consumerId)
}