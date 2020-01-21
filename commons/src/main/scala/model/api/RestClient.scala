package model.api


import io.vertx.core.buffer.Buffer
import io.vertx.scala.core.{MultiMap, Vertx}
import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse, WebClient}
import org.json4s.jackson.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object RestClient {
  def get(url: String,
          relativePath: String,
          requestParams: Option[Map[String, Any]] = None,
          port: Option[Int] = None,
          handleHeader: HttpRequest[_] => Unit = _ => {}): Future[Option[String]] = {

    val client: WebClient = WebClient.create(Vertx.vertx())

    val complexUri = new StringBuffer(relativePath)
    var first: Boolean = true
    requestParams match {
      case Some(params) =>
        params foreach { case (k, v) =>
          if (first) {
            complexUri.append("?" + k + "=" + v.toString)
            first = false
          } else {
            complexUri.append("&" + k + "=" + v.toString)
          }
        }
      case None =>
    }


    val future: HttpRequest[Buffer] = port match {
      case Some(p) => client.get(p, url, complexUri.toString)
      case None => client.get(url, complexUri.toString)
    }

    handleHeader(future)


    future.sendFuture().map { e => successRequest(e) }
  }

  private def successRequest(result: HttpResponse[Buffer]): Option[String] = {

    if (result.statusCode() == ResponseStatus.OK_CODE) {
      result.bodyAsString()
    } else {
      try {
        val errorMsg = read[Error](result.bodyAsString().get)
        throw new Throwable(errorMsg.message.getOrElse("No error description"))
      } catch {
        case _: Exception => throw new Throwable(result.bodyAsString().getOrElse(""))
      }
    }
  }

  def post(url: String,
           relativePath: String,
           requestParams: Option[Map[String, Any]] = None,
           port: Option[Int] = None): Future[Option[String]] = {

    implicit def toMultiMap(params: Map[String, Any]): MultiMap = {
      val form = MultiMap.caseInsensitiveMultiMap()
      params map { case (k, v) => form.set(k, v.toString) }

      form
    }

    val client: WebClient = WebClient.create(Vertx.vertx())

    val complexUri = new StringBuffer(relativePath)


    val future: HttpRequest[Buffer] = port match {
      case Some(p) => client.post(p, url, complexUri.toString)
      case None => client.post(url, complexUri.toString)
    }

    requestParams match {
      case Some(params) =>
        future.sendFormFuture(params) map { e => successRequest(e)}
      case None =>
        future.sendFuture() map { e => successRequest(e) }
    }

  }
}


