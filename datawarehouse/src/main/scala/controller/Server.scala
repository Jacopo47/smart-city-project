package controller

import io.vertx.core.http.HttpMethod
import io.vertx.scala.ext.web.RoutingContext
import model.api.{Dispatcher, RouterResponse}
import model.dao.FactTableComponent.FactTable
import model.logger.Log
import org.joda.time.DateTime
import slick.lifted.TableQuery

class Server(routes: Map[(String, HttpMethod), (RoutingContext, RouterResponse) => Unit]) extends Dispatcher(routes) {
  override def start(): Unit = {
    super.start()
    Log.info("Server is running...")
  }
}

object Server {
  def apply(): Server = {
    val handlers = Map(
      ("/datawarehouse/:from/:to/:zone", HttpMethod.GET) -> getTemperatures
    )

    new Server(handlers)
  }


  private def getTemperatures: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    val from = req.pathParams().getOrElse("from", "01/01/2000 00:00")
    val to = req.pathParams().getOrElse("to", DateTime.now().toString("dd/mm/yyyy"))
    val zone = req.pathParams().getOrElse("zone", "Cesena")

  }
}
