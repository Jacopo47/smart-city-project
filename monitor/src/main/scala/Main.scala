import controller.Server
import io.vertx.scala.core.Vertx

object Main {
  def main(args: Array[String]): Unit = {
    val port: Int = Option[String](System.getenv("PORT")).getOrElse("4700").toInt
    Vertx.vertx().deployVerticle(Server(port = port))
  }
}
