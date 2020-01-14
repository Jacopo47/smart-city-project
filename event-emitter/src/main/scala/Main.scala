import controller.EventEmitter
import model.logger.Log

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    Await.ready(EventEmitter().start(), Duration.Inf)

    Log.debug("Event-emitter shutdown!")
  }
}
