package model.api

import io.vertx.core.http.HttpMethod
import io.vertx.scala.ext.web.{Router, RoutingContext}
import model.api


/**
 * Interface for managing requests form the Vertx server.
 */
trait Request {
  def router: Router

  def url: String

  def method: HttpMethod

  def handle: (RoutingContext, RouterResponse) => Unit

  def handler(): Unit = {
    router.route(method, url).produces("application/json").handler(routingContext => {
      val res = api.RouterResponse(routingContext)
      handle(routingContext, res)
    })
  }
}


/**
 * GET request
 *
 * @param router
 *   The router object
 * @param url
 *   The relative path url
 * @param handle
 *   Request handler.
 */
case class GET(override val router: Router,
               override val url: String,
               override val handle: (RoutingContext, RouterResponse) => Unit) extends Request {
  override val method = HttpMethod.GET

  handler()
}

/**
 * POST request
 *
 * @param router
 *   The router object
 * @param url
 *   The relative path url
 * @param handle
 *   Request handler.
 */
case class POST(override val router: Router,
                override val url: String,
                override val handle: (RoutingContext, RouterResponse) => Unit) extends Request {
  override val method = HttpMethod.POST

  override def handler(): Unit = {
    router.post(url).produces("application/json").handler(routingContext => {
      routingContext.request().setExpectMultipart(true)
      routingContext.request().endHandler(_ =>{
        val res = api.RouterResponse(routingContext)
        handle(routingContext, res)
      })
    })
  }

  handler()
}


object ResponseStatus {

  val OK_CODE: Int = 200
  val EXCEPTION_CODE: Int = 409

  sealed trait HeaderStatus

  case object OK extends HeaderStatus

  case object ResponseException extends HeaderStatus


  /**
   * This method is used to get all the available seeds
   *
   * @return a Iterable containing all the available seeds.
   */
  def values: Iterable[HeaderStatus] = Iterable(OK, ResponseException)

}


