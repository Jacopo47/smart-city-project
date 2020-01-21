package model.dao

import java.sql.Timestamp

import model.api.ErrorStreamEntry
import model.utilities.UNKNOWN
import redis.clients.jedis.StreamEntry

import scala.collection.JavaConverters._

case class LogError(errorMsg: String, deviceId: String = model.utilities.UNKNOWN, zone: String = model.utilities.UNKNOWN) {

  def toMap: Map[String, String] = Map("error" -> errorMsg, "device" -> deviceId, "zone" -> zone)
}

object LogError {
  def getEntry(entry: StreamEntry): ErrorStreamEntry = {
    val datetime = new Timestamp(entry.getID.getTime)
    val fields = entry.getFields.asScala.toMap

    val error: String = fields.getOrElse("error", UNKNOWN)
    val device: String = fields.getOrElse("device", UNKNOWN)
    val zone: String = fields.getOrElse("zone", UNKNOWN)

    ErrorStreamEntry(datetime, LogError(error, device, zone))
  }
}
