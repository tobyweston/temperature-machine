package bad.robot.temperature

sealed abstract class Error(val message: String)

case class CrcFailure() extends Error("CRC failure, this could be caused by a physical interruption of signal due to shorts, a newly arriving 1-Wire device issuing a 'presence pulse' or gremlins.")
case class SensorError(details: String) extends Error(details)
case class UnexpectedError(details: String) extends Error(details)
case class FailedToFindFile(location: String) extends Error(s"Failed to find any 'w1_slave' sensor files in $location")
case class FileError(cause: Throwable) extends Error(s"Error loading file; ${cause.getMessage}")
case class Timeout(reason: String) extends Error(s"Timeout; $reason")
