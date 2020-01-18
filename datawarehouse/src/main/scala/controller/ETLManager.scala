package controller

import java.sql.Timestamp

import model.dao.ClientRedis.readStreamAsGroup
import model.dao.FactTableComponent.Fact
import model.dao.{ClientRedis, FactTableComponent, SENSOR_MAIN_STREAM_KEY, SensorRead}
import model.logger.Log
import org.joda.time.DateTime

import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.{Buffer => MBuffer}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Success


case class ETLManager(group: String, consumerId: String) {
  var temperatureEntries: Map[DateTime, MMap[String, MBuffer[Double]]] = Map[DateTime, MMap[String, MBuffer[Double]]]()
  ClientRedis.initializeConsumerGroup(group)

  FactTableComponent.defineSchema()

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

  private def onStreamEntry(entry: SensorRead): Unit = {
    Future {
      val entryHour: DateTime = entry.dateTime.hourOfDay().roundFloorCopy()

      temperatureEntries.get(entryHour) match {
        case Some(hourEntries) =>
          hourEntries.get(entry.name) match {
            case Some(entries) => entries += entry.temperature
            case None => hourEntries.put(entry.name, MBuffer(entry.temperature))
          }
        case None =>
          temperatureEntries = temperatureEntries + (entryHour -> MMap(entry.name -> MBuffer(entry.temperature)))
      }

      temperatureEntries
    } andThen {
      case _ => ClientRedis.sendAck(group, entry.id)
    } andThen {
      case Success(values) =>
        val currentHour = new DateTime().hourOfDay().roundFloorCopy().plusHours(1)
        val oldEntries: Map[DateTime, MMap[String, MBuffer[Double]]] = values.filter(e => e._1.isBefore(currentHour))

        oldEntries foreach { case (k, v) =>
          v foreach { case (zone, temperatures) =>
            val average = temperatures.sum / temperatures.size
            FactTableComponent.insert(Fact(zone, new Timestamp(k.getMillis), average))
          }

          temperatureEntries = temperatureEntries - (k)
        }

    }
  }
}