package bad.robot.temperature

sealed class Error(val message: String)

case class CrcFailure() extends Error("CRC failure, this could be caused by a physical interruption of signal due to shorts, a newly arriving 1-Wire device issuing a 'presence pulse' or gremlins.")
case class SensorError(details: String) extends Error(details)
case class UnexpectedError(details: String) extends Error(details)
