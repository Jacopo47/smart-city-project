import controller.EventEmitter
import model.dao.{ClientRedis, EVENT_EMITTER_CONSUMER_ID, EVENT_EMITTER_GROUP}
import model.logger.Log

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    Await.ready(EventEmitter( EVENT_EMITTER_GROUP, ClientRedis.getConsumerId(EVENT_EMITTER_CONSUMER_ID)).start(), Duration.Inf)

    Log.debug("Event-emitter shutdown!")
  }
}
