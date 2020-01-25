package controller

import java.lang
import java.sql.Timestamp

import io.lettuce.core.Consumer
import io.lettuce.core.XReadArgs.StreamOffset
import io.vertx.core.http.HttpMethod
import io.vertx.scala.ext.web.RoutingContext
import io.vertx.scala.ext.web.handler.CorsHandler
import model.api.{Dispatcher, Error, Errors, Message, Ok, RouterResponse, SimpleFact}
import model.dao.Granularity.GranularityState
import model.dao.{ClientRedis, ConsumerInfo, ERROR_STREAM_KEY, FactTableComponent, Granularity, LettuceRedis, LogError, SENSOR_MAIN_STREAM_KEY, SensorRead, StreamGroupsInfo, ToTimestamp}
import model.logger.Log
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class Server(routes: Map[(String, HttpMethod), (RoutingContext, RouterResponse) => Unit])
  extends Dispatcher(routes, handler = CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)) {

  override def start(): Unit = {
    super.start()
    Log.info("Server is running...")
  }
}

object Server {
  def apply(): Server = {
    val handlers = Map(
      ("/datawarehouse/:from/:to/:zone/:granularity", HttpMethod.GET) -> getTemperatures,
      ("/api/errors", HttpMethod.GET) -> latestErrors,
      ("/api/consumersInfo/:group", HttpMethod.GET) -> allConsumerInfo,
      ("/api/data/:zone/:limit", HttpMethod.GET) -> getData,
      ("/api/zone/", HttpMethod.GET) -> getZones,
      ("/api/zone/last", HttpMethod.GET) -> getLatestZoneRead,
      ("/api/consumerGroup/info", HttpMethod.GET) -> consumerGroupInfo,
      ("/api/consumerGroup/destroy/:group", HttpMethod.POST) -> destroyGroup,
      ("/api/consumerGroup/remove/:group/:consumer", HttpMethod.POST) -> deleteConsumer,
      ("/api/consumerGroup/set-id/:group/:id", HttpMethod.POST) -> setConsumerGroupId
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
    val groups = LettuceRedis {

      _.xinfoGroups(SENSOR_MAIN_STREAM_KEY)
    }.asScala
      .map(e => StreamGroupsInfo(e))

    groups foreach {
      g =>
        g.consumersList = Some(LettuceRedis {
          _.xinfoConsumers(SENSOR_MAIN_STREAM_KEY, g.name)
        }.asScala.map(e => ConsumerInfo(e)))
    }

    res.sendResponse(Ok(groups))
  }

  def allConsumerInfo: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    req.pathParams().get("group") match {
      case Some(group) =>
        val result = LettuceRedis {
          _.xinfoConsumers(SENSOR_MAIN_STREAM_KEY, group)
        }
        res.sendResponse(Ok(result
          .asScala
          .map(e => ConsumerInfo(e))))

      case None => res.sendResponse(Error(Some("Please specify a group")))
    }
  }

  private def getData: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    req.pathParam("zone") match {
      case Some(zone) =>
        val limit: Int = req.pathParam("limit").getOrElse("10").toInt

        res
          .sendResponse(
            Ok(ClientRedis {
              _.xrevrange(SENSOR_MAIN_STREAM_KEY, null, null, limit)
            }
              .asScala
              .map(SensorRead(_))
              .filter(e => e.name.equalsIgnoreCase(zone))))
      case None => res.sendResponse(Error(Some("Please specify a zone")))
    }
  }

  private def getZones: (RoutingContext, RouterResponse) => Unit = (_, res) => {

    res
      .sendResponse(
        Ok(ClientRedis {
          _.xrevrange(SENSOR_MAIN_STREAM_KEY, null, null, Int.MaxValue)
        }
          .asScala
          .map(SensorRead(_))
          .map(_.zone).toSet))

  }

  private def getLatestZoneRead: (RoutingContext, RouterResponse) => Unit = (_, res) => {
    res
      .sendResponse(
        Ok(ClientRedis {
          _.xrevrange(SENSOR_MAIN_STREAM_KEY, null, null, Int.MaxValue)
        }
          .asScala
          .map(SensorRead(_))
          .groupBy(_.zone).map(_._2.head)))

  }

  private def getTemperatures: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
    val from: Timestamp = formatter.parseDateTime(req.pathParams().getOrElse("from", "01/01/2000"))
    val to: Timestamp = formatter.parseDateTime(req.pathParams().getOrElse("to", DateTime.now().toString("dd/mm/yyyy")))
    val zone = req.pathParams().getOrElse("zone", "Cesena")
    val granularity: GranularityState = Granularity.valueOf(req.pathParams().getOrElse("granularity", "day"))


    FactTableComponent.select(from, to, zone, granularity) onComplete {
      case Success(values) => res.sendResponse(Ok(values.map(e => SimpleFact(zone, e._1, e._2))))
      case Failure(exception) => res.sendResponse(Error(Some(exception.getMessage)))
    }
  }

  private def destroyGroup: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    req.pathParam("group") match {
      case Some(group) =>
        try {
          if (LettuceRedis {
            _.xgroupDestroy(SENSOR_MAIN_STREAM_KEY, group)
          }) {
            res.sendResponse(Message("Group correctly deleted!"))
          } else {
            res.sendResponse(Error(Some("Error! Impossible delete the group..")))
          }
        } catch {
          case ex: Throwable => res.sendResponse(Error(Some("Error! Impossible delete the group. Details: " + ex.getMessage)))
        }
      case None => res.sendResponse(Error(Some("Please specify the group to delete")))
    }
  }

  private def deleteConsumer: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    req.pathParam("group") match {
      case Some(group) =>
        req.pathParam("consumer") match {
          case Some(consumer) =>
            try {
              if (LettuceRedis {
                _.xgroupDelconsumer(SENSOR_MAIN_STREAM_KEY, Consumer.from(group, consumer))
              }) {
                res.sendResponse(Message("Consumer correctly deleted!"))
              } else {
                res.sendResponse(Error(Some("Error! Impossible delete the consumer..")))
              }
            } catch {
              case ex: Throwable => res.sendResponse(Error(Some("Error! Impossible delete the consumer. Details: " + ex.getMessage)))
            }
          case None =>
            res.sendResponse(Error(Some("Please specify the consumer name")))
        }

      case None => res.sendResponse(Error(Some("Please specify the group of the consumer")))
    }
  }

  private def setConsumerGroupId: (RoutingContext, RouterResponse) => Unit = (req, res) => {
    req.pathParam("group") match {
      case Some(group) =>
        req.pathParam("id") match {
          case Some(id) =>
            try {
              val result: String =LettuceRedis {
                _.xgroupSetid(StreamOffset.from(SENSOR_MAIN_STREAM_KEY, id), group)
              }
              if(result.equalsIgnoreCase("OK")) {
                res.sendResponse(Message("Consumer correctly deleted!"))
              } else {
                res.sendResponse(Error(Some("Error! Impossible set the ID -> " + id + ". Details: " + result)))
              }
            } catch {
              case ex: Throwable => res.sendResponse(Error(Some("Error! Impossible set the ID -> " + id + ". Details: " + ex.getMessage)))
            }
          case None =>
            res.sendResponse(Error(Some("Please specify the ID to set")))
        }

      case None => res.sendResponse(Error(Some("Please specify the group")))
    }
  }
}
