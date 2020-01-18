package model

import java.text.SimpleDateFormat

import org.json4s.DefaultFormats
import redis.clients.jedis.Jedis

package object dao {
  implicit val formats: DefaultFormats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  }

  def STREAM_MAX_LENGTH = 500

  def EVENT_EMITTER_GROUP = "event-emitter"
  def EVENT_EMITTER_CONSUMER_ID = "event:emitter:consumer:id"

  def UPDATER_GROUP = "updater"
  def UPDATER_CONSUMER_ID = "updater:consumer:id"

  def DATA_WAREHOUSE_GROUP = "data-warehouse"
  def DATA_WAREHOUSE_CONSUMER_ID = "data:warehouse:consumer:id"

  def FORECASTING_GROUP = "forecasting-group"
  def FORECASTING_CONSUMER_ID = "forecasting:consumer:id"

  def SENSOR_MAIN_STREAM_KEY = "sensor:stream"
  def ERROR_STREAM_KEY = "sensor:errors:stream"

  def closeConnection(client: Jedis): Unit = client.quit()
}

