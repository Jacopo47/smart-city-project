import com.corundumstudio.socketio.{Configuration, SocketIOServer}
import controller.Updater
import model.dao.{ClientRedis, UPDATER_CONSUMER_ID, UPDATER_GROUP}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  val config = new Configuration()
  config.setHostname("localhost")
  config.setPort(9092)

  val server = new SocketIOServer(config)

  server.start()

  Await.ready(Updater(server, UPDATER_GROUP, ClientRedis.getConsumerId(UPDATER_CONSUMER_ID)).start(), Duration.Inf)

  server.stop()
}