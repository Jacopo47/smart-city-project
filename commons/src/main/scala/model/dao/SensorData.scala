package model.dao

import java.sql.Timestamp

import model.dao.SensorData._
import model.utilities.UNKNOWN
import org.joda.time.DateTime
import org.json4s.jackson.Serialization.{read, write}
import redis.clients.jedis.StreamEntry


import scala.collection.JavaConverters._

class SensorData(val name: String, val zone: String, val temperature: Double, val humidity: Double, val coordinate: Option[Coordinate]) {

  override def toString: String = write(this)

  def toMap: Map[String, String] = {
    coordinate match {
      case Some(c) => Map(DEVICE_NAME -> name, ZONE -> zone,TEMPERATURE -> temperature.toString, HUMIDITY -> humidity.toString, LATITUDE -> c.latitude.toString, LONGITUDE -> c.longitude.toString)
      case None => Map(DEVICE_NAME -> name, ZONE -> zone,TEMPERATURE -> temperature.toString, HUMIDITY -> humidity.toString)
    }
  }
}

class SensorRead(val id: String,
                 val dateTime: Timestamp,
                 override val name: String,
                 override val zone: String,
                 override val temperature: Double,
                 override val humidity: Double,
                 override val coordinate: Option[Coordinate]) extends SensorData(name, zone, temperature, humidity, coordinate) {

}

object SensorRead {
  def apply(entry: StreamEntry): SensorRead = {
    val datetime = new DateTime(entry.getID.getTime)
    val fields = entry.getFields.asScala.toMap

    val name: String = fields.getOrElse(DEVICE_NAME, UNKNOWN)
    val zone: String = fields.getOrElse(ZONE, UNKNOWN)
    val temperature: Double = fields.getOrElse(TEMPERATURE, "0").toDouble
    val humidity: Double = fields.getOrElse(HUMIDITY, "0").toDouble
    val latitude: Double = fields.getOrElse(LATITUDE, "0").toDouble
    val longitude: Double = fields.getOrElse(LONGITUDE, "0").toDouble

    new SensorRead(entry.getID.toString, datetime, name, zone, temperature, humidity, Some(Coordinate(latitude, longitude)))
  }
}


object SensorData {
  def DEVICE_NAME = "name"

  def TEMPERATURE = "temperature"

  def HUMIDITY = "humidity"

  def LATITUDE = "latitude"

  def LONGITUDE = "longitude"

  def ZONE = "zone"

  def apply(dataAsJson: String, deviceName: String): SensorData = {
    val app: SensorDataRead = read[SensorDataRead](dataAsJson)
    val data: SensorDataRead = SensorDataRead(app.coord, app.main, deviceName, app.name)

    def kelvinToCelsius(temp: Double): Double = (temp - 273.15).round

    SensorData(data.name, data.zone, kelvinToCelsius(data.main.temp), data.main.humidity, Some(Coordinate(data.coord.lat, data.coord.lon)))
  }

  def apply(name: String, zone: String, temperature: Double, humidity: Double, coordinate: Option[Coordinate] = None): SensorData = new SensorData(name, zone,temperature, humidity, coordinate)
}


case class Coordinate(latitude: Double, longitude: Double)

case class SensorDataRead(coord: CoordinateRead, main: MainRead, name: String, zone: String = UNKNOWN)

case class CoordinateRead(lon: Double, lat: Double)

case class MainRead(temp: Double, humidity: Double)

