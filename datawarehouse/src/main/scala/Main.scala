import controller.ETLManager
import model.dao.{ClientRedis, DATA_WAREHOUSE_CONSUMER_ID, DATA_WAREHOUSE_GROUP}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {

    val consumerId = ClientRedis { _.incr(DATA_WAREHOUSE_CONSUMER_ID)}
    Await.ready(ETLManager(DATA_WAREHOUSE_GROUP, consumerId.toString).start(), Duration.Inf)
  }
}
