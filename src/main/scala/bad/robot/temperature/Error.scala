package bad.robot.temperature

sealed class Error(val message: String)

case class SensorError(details: String) extends Error(details)
