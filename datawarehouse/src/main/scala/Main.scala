import controller.Server
import io.vertx.scala.core.Vertx

object Main {
  def main(args: Array[String]): Unit = {
    Vertx.vertx().deployVerticle(Server())
  }
}
