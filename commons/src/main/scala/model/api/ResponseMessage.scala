package model.api

import java.sql.Timestamp

import model.dao.FactTableComponent.Fact
import model.dao.LogError

/**
 * This class is used for define the message accepted to the RouterResponse.
 *
 */

sealed trait JsonResponse

case class Message(message: String) extends JsonResponse
case class Error(message: Option[String] = None) extends JsonResponse
case class ResponseArray(list: Seq[Any]) extends JsonResponse
case class Facts(facts: Seq[Fact]) extends JsonResponse
case class ErrorStreamEntry(dateTime: Timestamp, error: LogError) extends JsonResponse
case class Errors(errors: Seq[ErrorStreamEntry]) extends JsonResponse
case class Ok[T](data: T) extends JsonResponse