package controller

import model.dao.ClientRedis.readStreamAsGroup
import model.dao.{ClientRedis, EVENT_EMITTER_GROUP, SENSOR_MAIN_STREAM_KEY, SensorRead}
import model.logger.Log

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

case class EventEmitter() {


  ClientRedis.initializeConsumerGroup(model.dao.EVENT_EMITTER_GROUP)

  def start(): Future[Unit] = {
    val consumerId: String = ClientRedis(db => {
      db.incr(model.dao.EVENT_EMITTER_CONSUMER_ID)
    }).toString

    Future {
      while (true) {
        try {
          readStreamAsGroup(SENSOR_MAIN_STREAM_KEY, EVENT_EMITTER_GROUP, consumerId) match {
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

  private def onStreamEntry(entry: SensorRead): Unit = {
    Future {
      if (entry.temperature > 40) {
        Log.warn(s"Zone ${entry.name} hot alarm!! Check as soon as possible!!")
        //TODO Aggiungere update tramite socket.io
      }

      if (entry.temperature < -10) {
        Log.warn(s"Zone ${entry.name} cold alarm!! Check as soon as possible!!")
        //TODO Aggiungere update tramite socket.io
      }

      if (entry.humidity <= 0) {
        Log.warn(s"Zone ${entry.name} possibile humidity fail relevation!! Check as soon as possible!!")
        //TODO Aggiungere update tramite socket.io
      }
    } andThen {
      case _ => ClientRedis.sendAck(EVENT_EMITTER_GROUP, entry.id)
    }
  }

}