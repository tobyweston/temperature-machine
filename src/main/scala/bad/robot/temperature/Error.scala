package bad.robot.temperature

sealed abstract class Error(val message: String)

case class CrcFailure() extends Error("CRC failure, this could be caused by a physical interruption of signal due to shorts, a newly arriving 1-Wire device issuing a 'presence pulse' or gremlins.")
case class SensorError(details: String) extends Error(details)
case class UnexpectedError(details: String) extends Error(details)
case class CommandLineError() extends Error("""|Usage: Server <hosts>
                                               |Please supply at least one source host, e.g. 'Server bedroom lounge'
                                               |""".stripMargin)
case class FailedToFindFile(location: String) extends Error(s"Failed to find any 'w1_slave' sensor files in $location")
case class FileError(cause: Throwable) extends Error(s"Error loading file; ${cause.getMessage}")
case class Timeout(reason: String) extends Error(s"Timeout; $reason")
case class ParseError(reason: String) extends Error(s"Unable to parse content as JSON $reason")
case class RrdError(reason: String) extends Error(s"Error in RRD $reason")
case class SensorSpikeError(spikes: List[Spike]) extends Error(
  s"""An unexpected spike was encountered on:
     | sensor(s)             : ${spikes.map(_.sensor).mkString(", ")}
     | previous temperatures : ${spikes.map(_.previous).map(_.asCelsius).mkString(", ")}
     | spiked temperatures   : ${spikes.map(_.current).map(_.asCelsius).mkString(", ")}
     |""".stripMargin
)
case class ConfigurationError(details: String) extends Error(details)