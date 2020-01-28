import com.corundumstudio.socketio.{Configuration, SocketIOServer}
import controller.Updater
import model.dao.{ClientRedis, UPDATER_CONSUMER_ID, UPDATER_GROUP}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  val config = new Configuration()

  val host: String = Option(System.getenv("WS_HOST")).getOrElse("localhost")
  val port: Int = Option(System.getenv("PORT")).getOrElse("9092").toInt
  config.setHostname(host)
  config.setPort(port)

  val server = new SocketIOServer(config)

  server.start()

  Await.ready(Updater(server, UPDATER_GROUP, ClientRedis.getConsumerId(UPDATER_CONSUMER_ID)).start(), Duration.Inf)

  server.stop()
}