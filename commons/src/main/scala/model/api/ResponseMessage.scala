package model.api

import model.dao.FactTableComponent.Fact

/**
 * This class is used for define the message accepted to the RouterResponse.
 *
 */

sealed trait JsonResponse

case class Message(message: String) extends JsonResponse
case class Error(cause: Option[String] = None) extends JsonResponse
case class ResponseArray(list: Seq[Any]) extends JsonResponse
case class Facts(facts: Seq[Fact]) extends JsonResponse