package model.dao


case class LogError( errorMsg: String, deviceId: String = model.utilities.UNKNOWN, zone: String = model.utilities.UNKNOWN) {

  def toMap: Map[String, String] = Map("error" -> errorMsg, "device" -> deviceId, "zone" -> zone)
}
