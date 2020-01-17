package controller

import java.sql.Timestamp
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import io.vertx.core.http.HttpMethod
import io.vertx.scala.ext.web.RoutingContext
import model.api.{Dispatcher, Error, Errors, Facts, Ok, RouterResponse}
import model.dao.{ClientRedis, ConsumerInfo, ERROR_STREAM_KEY, FactTableComponent, LettuceRedis, LogError, SENSOR_MAIN_STREAM_KEY, StreamGroupsInfo}
import model.logger.Log

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


class Server(routes: Map[(String, HttpMethod), (RoutingContext, RouterResponse) => Unit]) extends Dispatcher(routes) {
  override def start(): Unit = {
    super.start()
    Log.info("Server is running...")
  }
}

object Server {
  def apply(): Server = {
    val handlers = Map(
      ("/datawarehouse/:from/:to/:zone", HttpMethod.GET) -> getTemperatures,
      ("/api/errors", HttpMethod.GET) -> latestErrors,
      ("/api/consumerGroupInfo", HttpMethod.GET) -> consumerGroupInfo,
      ("/api/consumersInfo/:group", HttpMethod.GET) -> allConsumerInfo
    )

    new Server(handlers)
  }


  def latestErrors: (RoutingContext, RouterResponse) => Unit = (_, res) => {
    val result = ClientRedis {
      _.xrevrange(ERROR_STREAM_KEY, null, null, 10)
    }.asScala.map(LogError getEntry)

    res.sendResponse(Errors(result))
  }

  def consumerGroupInfo: (RoutingContext, RouterResponse) => Unit = (_, res) => {
    val result = LettuceRedis {

      _.xinfoGroups(SENSOR_MAIN_STREAM_KEY)
    }

    res.sendResponse(Ok(result
      .asScala
      .map(e => StreamGroupsInfo(e))))
  }

  def allConsumerInfo: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    req.pathParams().get("group") match {
      case Some(group) =>
        val result = LettuceRedis { _.xinfoConsumers(SENSOR_MAIN_STREAM_KEY, group) }
        res.sendResponse(Ok(result
          .asScala
          .map(e => ConsumerInfo(e))))

      case None => res.sendResponse(Error(Some("Please specify a group")))
    }
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
