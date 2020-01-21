package controller


import java.sql.Timestamp
import java.util.Date

import com.corundumstudio.socketio.SocketIOServer
import model.api.{ErrorStreamEntry, Errors}
import model.dao._
import model.logger.Log
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class Updater(server: SocketIOServer, group: String, consumerId: String) {

  implicit val formats: DefaultFormats.type = DefaultFormats

  ClientRedis.initializeConsumerGroup(group)
  ClientRedis.initializeConsumerGroup(group, ERROR_STREAM_KEY)



  private def onStreamEntry(entry: SensorRead): Unit = {
    val message: SensorReadMessageUpdate = SensorReadMessageUpdate(entry.name, entry.temperature, entry.humidity, entry.dateTime)
    server.getBroadcastOperations.sendEvent("sensor-read-update", write(message))
  }

  private def onErrorStreamEntry(entry: ErrorStreamEntry): Unit = {
    server.getBroadcastOperations.sendEvent("sensor-error-update", write(Errors(Seq(entry))))
  }


  def start(): Future[Unit] = {
    Log.debug("Updater is running...")
    Future {
      while (true) {
        try {
          ClientRedis.readStreamAsGroup(SENSOR_MAIN_STREAM_KEY, group, consumerId) match {
            case Some(data) =>
              data._2.map(e => SensorRead(e)).foreach(onStreamEntry)
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
              data._2.map(LogError getEntry).foreach(onErrorStreamEntry)
            case None =>
              Log.debug(s"Nessun dato da leggere da $group...")
          }
        } catch {
          case e: Throwable => Log error e
        }
      }
    }
  }

}

case class SensorReadMessageUpdate(zone: String, temperature: Double, humidity: Double, dateTime: Timestamp)

object Updater {
  def apply(server: SocketIOServer, group: String, consumerId: String): Updater = new Updater(server, group, consumerId)
}