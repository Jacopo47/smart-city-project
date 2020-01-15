package model.dao

import model.dao.SensorData._
import model.utilities.UNKNOWN
import org.joda.time.DateTime
import org.json4s.jackson.Serialization.{read, write}
import redis.clients.jedis.{StreamEntry, StreamEntryID}

import scala.jdk.CollectionConverters._

class SensorData(val name: String, val temperature: Double, val humidity: Double, val coordinate: Option[Coordinate]) {

  override def toString: String = write(this)

  def toMap: Map[String, String] = {
    coordinate match {
      case Some(c) => Map(DEVICE_NAME -> name, TEMPERATURE -> temperature.toString, HUMIDITY -> humidity.toString, LATITUDE -> c.latitude.toString, LONGITUDE -> c.longitude.toString)
      case None => Map(DEVICE_NAME -> name, TEMPERATURE -> temperature.toString, HUMIDITY -> humidity.toString)
    }
  }
}

class SensorRead(val id: StreamEntryID,
                 val dateTime: DateTime,
                 override val name: String,
                 override val temperature: Double,
                 override val humidity: Double,
                 override val coordinate: Option[Coordinate]) extends SensorData(name, temperature, humidity, coordinate) {

}

object SensorRead {
  def apply(entry: StreamEntry): SensorRead = {
    val datetime = new DateTime(entry.getID.getTime)
    val fields = entry.getFields.asScala.toMap

    val name: String = fields.getOrElse(DEVICE_NAME, UNKNOWN)
    val temperature: Double = fields.getOrElse(TEMPERATURE, "0").toDouble
    val humidity: Double = fields.getOrElse(HUMIDITY, "0").toDouble
    val latitude: Double = fields.getOrElse(LATITUDE, "0").toDouble
    val longitude: Double = fields.getOrElse(LONGITUDE, "0").toDouble

    new SensorRead(entry.getID, datetime, name, temperature, humidity, Some(Coordinate(latitude, longitude)))
  }
}


object SensorData {
  def DEVICE_NAME = "name"

  def TEMPERATURE = "temperature"

  def HUMIDITY = "humidity"

  def LATITUDE = "latitude"

  def LONGITUDE = "longitude"

  def apply(dataAsJson: String): SensorData = {
    val data: SensorDataRead = read[SensorDataRead](dataAsJson)

    def fahrenheitToCelsius(temp: Double): Double = (temp - 32) * (5/9)

    SensorData(data.name, fahrenheitToCelsius(data.main.temp), data.main.humidity, Some(Coordinate(data.coord.lat, data.coord.lon)))
  }

  def apply(name: String, temperature: Double, humidity: Double, coordinate: Option[Coordinate] = None): SensorData = new SensorData(name, temperature, humidity, coordinate)
}


case class Coordinate(latitude: Double, longitude: Double)

case class SensorDataRead(coord: CoordinateRead, main: MainRead, name: String)

case class CoordinateRead(lon: Double, lat: Double)

case class MainRead(temp: Double, humidity: Double)

