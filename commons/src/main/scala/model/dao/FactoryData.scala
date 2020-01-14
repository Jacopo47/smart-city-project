package model.dao

import model.dao.ControllerMode.ControllerMode
import model.dao.EmergencyStopState.EmergencyStopState
import model.dao.ExecutionState.ExecutionState
import model.utilities.{NULL_DATA, UNAVAILABLE}
import org.joda.time.DateTime
import redis.clients.jedis.StreamEntry

import scala.collection.JavaConverters._

class FactoryData(val datetimeSource: DateTime, val partCount: Long, controllerMode: ControllerMode, toolNumber: Double, val machineName: String, block: String, val rotaryVelocity: Double, val rotaryTemperature: Double, val execution: ExecutionState, val program: String, val emergencyStop: EmergencyStopState) {

  def getHardwareData = HardwareData(datetimeSource.toString("HH:mm"), rotaryVelocity, rotaryTemperature)

  override def toString: String = "SourceTime: " + datetimeSource + "\n" +
    "MACHINE -> " + machineName + "\n" +
    "Program: " + program + "\n" +
    "PartCount: " + partCount + "\n" +
    "ControllerMode: " + controllerMode + "\n" +
    "ToolNumber: " + toolNumber + "\n" +
    "Block: " + block + "\n" +
    "RotaryVelocity: " + rotaryVelocity + "\n" +
    "RotaryTemperature: " + rotaryTemperature + "\n" +
    "ExecutionState: " + execution + "\n" +
    "EmergencyStop: " + emergencyStop
}

case class HardwareData(time: String, rotaryVelocity: Double, temperature: Double)

object FactoryData {
  val PART_COUNT = "PartCount"
  val CONTROLLER_MODE = "ControllerMode"
  val TOOL_NUMBER = "ToolNumber"
  val MACHINE_NAME = "machine"
  val BLOCK = "Block"
  val PROGRAM = "Program"
  val EXECUTION = "Execution"
  val ROTARY_VELOCITY = "RotaryVelocity"
  val ROTARY_TEMPERATURE = "RotaryTemperature"
  val EMERGENCY_STOP = "EmergencyStop"

  def apply(entry: StreamEntry): FactoryData = {
    val datetime = new DateTime(entry.getID.getTime)
    val fields = entry.getFields.asScala.toMap

    val partCount = fields.dataOrElse(PART_COUNT, "0", checkUnavailable = true).toLong
    val controllerMode = ControllerMode(fields.dataOrElse(CONTROLLER_MODE, ControllerMode.UNAVAILABLE.asString))
    val toolNumber: Double = fields.dataOrElse(TOOL_NUMBER, "0", checkUnavailable = true).toDouble
    val machineName = fields.dataOrElse(MACHINE_NAME, UNAVAILABLE)
    val block = fields.dataOrElse(BLOCK, UNAVAILABLE)
    val program = fields.dataOrElse(PROGRAM, UNAVAILABLE)
    val rotaryVelocity = fields.dataOrElse(ROTARY_VELOCITY, "0", checkUnavailable = true).toDouble
    val rotaryTemperature = fields.dataOrElse(ROTARY_TEMPERATURE, "0", checkUnavailable = true).toDouble
    val executionState = ExecutionState(fields.dataOrElse(EXECUTION, ExecutionState.UNAVAILABLE.asString))
    val emergencyStop: EmergencyStopState = EmergencyStopState(fields.dataOrElse(EMERGENCY_STOP, EmergencyStopState.UNAVAILABLE.asString))

    new FactoryData(datetime, partCount, controllerMode, toolNumber, machineName, block, rotaryVelocity, rotaryTemperature, executionState, program, emergencyStop)
  }

  private implicit class fromMapOrElse(map: Map[String, String]) {

    def dataOrElse (field: String, alternative: String, checkUnavailable: Boolean = false): String = {
      val value = map.getOrElse(field, alternative)

      if (value.equalsIgnoreCase(NULL_DATA)) return alternative

      if (checkUnavailable && value.equalsIgnoreCase(UNAVAILABLE)) return alternative

      value
    }
  }
}


object ControllerMode {

  sealed trait ControllerMode {
    def asString: String
  }

  def apply(input: String): ControllerMode = valueOf(input)

  case object AUTOMATIC extends ControllerMode {
    override val asString: String = "AUTOMATIC"
  }

  case object MANUAL extends ControllerMode {
    override val asString: String = "MANUAL"
  }

  case object MANUAL_DATA_INPUT extends ControllerMode {
    override val asString: String = "MANUAL_DATA_INPUT"
  }

  case object SEMI_AUTOMATIC extends ControllerMode {
    override val asString: String = "SEMI_AUTOMATIC"
  }

  case object EDIT extends ControllerMode {
    override val asString: String = "EDIT"
  }

  case object UNAVAILABLE extends ControllerMode {
    override val asString: String = "UNAVAILABLE"
  }


  /**
    * This method is used to get all the available seeds
    *
    * @return a Iterable containing all the available seeds.
    */
  def values: Iterable[ControllerMode] = Iterable(AUTOMATIC, MANUAL, MANUAL_DATA_INPUT, SEMI_AUTOMATIC, EDIT, UNAVAILABLE)

  def valueOf(input: String): ControllerMode = input match {
    case AUTOMATIC.asString => AUTOMATIC
    case MANUAL.asString => MANUAL
    case MANUAL_DATA_INPUT.asString => MANUAL_DATA_INPUT
    case SEMI_AUTOMATIC.asString => SEMI_AUTOMATIC
    case EDIT.asString => EDIT
    case UNAVAILABLE.asString => UNAVAILABLE
    case _ => throw new Exception("No match found")
  }
}


object ExecutionState {

  sealed trait ExecutionState {
    def asString: String
  }

  def apply(input: String): ExecutionState = valueOf(input)

  case object ACTIVE extends ExecutionState {
    override val asString: String = "ACTIVE"
  }

  case object READY extends ExecutionState {
    override val asString: String = "READY"
  }

  case object INTERRUPTED extends ExecutionState {
    override val asString: String = "INTERRUPTED"
  }

  case object FEED_HOLD extends ExecutionState {
    override val asString: String = "FEED_HOLD"
  }

  case object STOPPED extends ExecutionState {
    override val asString: String = "STOPPED"
  }

  case object OPTIONAL_STOP extends ExecutionState {
    override val asString: String = "OPTIONAL_STOP"
  }

  case object PROGRAM_STOPPED extends ExecutionState {
    override val asString: String = "PROGRAM_STOPPED"
  }

  case object PROGRAM_COMPLETED extends ExecutionState {
    override val asString: String = "PROGRAM_COMPLETED"
  }

  case object UNAVAILABLE extends ExecutionState {
    override val asString: String = "UNAVAILABLE"
  }


  /**
    * This method is used to get all the available seeds
    *
    * @return a Iterable containing all the available seeds.
    */
  def values: Iterable[ExecutionState] = Iterable(ACTIVE, READY, INTERRUPTED, FEED_HOLD, STOPPED, OPTIONAL_STOP, PROGRAM_STOPPED, PROGRAM_COMPLETED, UNAVAILABLE)

  def valueOf(input: String): ExecutionState = input.toUpperCase match {
    case ACTIVE.asString => ACTIVE
    case READY.asString => READY
    case INTERRUPTED.asString => INTERRUPTED
    case FEED_HOLD.asString => FEED_HOLD
    case STOPPED.asString => STOPPED
    case OPTIONAL_STOP.asString => OPTIONAL_STOP
    case PROGRAM_STOPPED.asString => PROGRAM_STOPPED
    case PROGRAM_COMPLETED.asString => PROGRAM_COMPLETED
    case UNAVAILABLE.asString => UNAVAILABLE
    case _ => throw new Exception("No match found")
  }
}


object EmergencyStopState {

  sealed trait EmergencyStopState {
    def asString: String
  }

  def apply(input: String): EmergencyStopState = valueOf(input)

  case object ARMED extends EmergencyStopState {
    override val asString: String = "AUTOMATIC"
  }

  case object TRIGGERED extends EmergencyStopState {
    override val asString: String = "MANUAL"
  }

  case object UNAVAILABLE extends EmergencyStopState {
    override val asString: String = "UNAVAILABLE"
  }

  /**
    * This method is used to get all the available seeds
    *
    * @return a Iterable containing all the available seeds.
    */
  def values: Iterable[EmergencyStopState] = Iterable(ARMED, TRIGGERED, UNAVAILABLE)

  def valueOf(input: String): EmergencyStopState = input match {
    case ARMED.asString => ARMED
    case TRIGGERED.asString => TRIGGERED
    case UNAVAILABLE.asString => UNAVAILABLE
    case _ => throw new Exception("No match found")
  }
}
