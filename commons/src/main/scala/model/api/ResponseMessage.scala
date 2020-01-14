package model.api

/**
 * This class is used for define the message accepted to the RouterResponse.
 *
 */

sealed trait JsonResponse

case class Message(message: String) extends JsonResponse
case class Error(cause: Option[String] = None) extends JsonResponse
case class MachineHardwareData(machine: String, times: Seq[String], velocity: Seq[Double], temperatures: Seq[Double])
case class MachinesHardwareData(machines: Seq[MachineHardwareData]) extends JsonResponse
case class ExecutionStateData(state: String, valueAsMinutes: Int)
case class ExecutionTime(machine: String, states: Seq[String], values: Seq[Int])
case class MachinesExecutionTime(machines: Seq[ExecutionTime]) extends JsonResponse
case class ProgramData(programName: String, valuesAsPieces: Long)
case class Program(machine: String, program: Seq[ProgramData])
case class MachinesPrograms(machines: Seq[Program]) extends JsonResponse
case class ResponseArray(list: Seq[Any]) extends JsonResponse