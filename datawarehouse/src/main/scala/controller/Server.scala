package controller

import java.sql.Timestamp

import io.vertx.core.http.HttpMethod
import io.vertx.scala.ext.web.RoutingContext
import model.api.{Dispatcher, Error, Facts, Message, RouterResponse}
import model.dao.FactTableComponent
import model.dao.FactTableComponent.Fact
import model.logger.Log
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Failure, Success}

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
    val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
    val from = new Timestamp(formatter.parseDateTime(req.pathParams().getOrElse("from", "01/01/2000")).getMillis)
    val to = new Timestamp(formatter.parseDateTime(req.pathParams().getOrElse("to", DateTime.now().toString("dd/mm/yyyy"))).getMillis)
    val zone = req.pathParams().getOrElse("zone", "Cesena")


    FactTableComponent.select(from, to, zone) onComplete {
      case Success(values) => res.sendResponse(Facts(values))
      case Failure(exception) => res.sendResponse(Error(Some(exception.getMessage)))
    }
  }
}
