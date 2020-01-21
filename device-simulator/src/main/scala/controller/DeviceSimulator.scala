package controller

import controller.DeviceSimulator.DEVICE_NAME
import model.api.RestClient
import model.dao.{ClientRedis, SensorData}
import model.logger.Log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class DeviceSimulator() {
  def start(): Future[Unit] = {
    Future {
      while (true) {
        Thread.sleep(DeviceSimulator.DEVICE_TIME_SLEEP)

        RestClient
          .get(DeviceSimulator.API_ENDPOINT,
            DeviceSimulator.API_PATH,
            requestParams = Some(Map("q" -> DeviceSimulator.DEVICE_LOCATION, "appid" -> DeviceSimulator.API_KEY)))
          .onComplete {
            case Success(data) =>
              onDataEntry(SensorData(data.getOrElse("No data found"), DEVICE_NAME))
            case Failure(exception) =>
              Log error exception
          }
      }
    }
  }

  def onDataEntry(data: SensorData): Unit = {
    ClientRedis.defaultAddToMainStream(data.toMap)
  }

}

object DeviceSimulator {
  def DEVICE_TIME_SLEEP = 10000

  def API_ENDPOINT = "api.openweathermap.org"

  def API_PATH: String = "/data/" + API_VERSION + "/weather"

  def API_VERSION = "2.5"

  def DEVICE_LOCATION: String = System.getenv("deviceLocation")

  def DEVICE_NAME: String = System.getenv("deviceName")

  def API_KEY: String = System.getenv("weatherApiKey")
}
