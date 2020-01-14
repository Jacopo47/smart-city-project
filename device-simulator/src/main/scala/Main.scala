import controller.DeviceSimulator
import model.logger.Log

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    Await.ready(DeviceSimulator().start(), Duration.Inf)

    Log.debug("Device simulator shutdown!")
  }
}
