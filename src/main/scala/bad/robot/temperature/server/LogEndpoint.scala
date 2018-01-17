package bad.robot.temperature.server

import java.util.Scanner

import bad.robot.temperature.rrd.{RrdFile, _}
import bad.robot.temperature.{Error, FileError, LogMessage, LogParser}
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz.\/.{fromTryCatchNonFatal, _}
import scalaz.{\/, \/-}


object LogEndpoint {
  
  private val toLogMessage: String => Error \/ LogMessage = line => LogParser.parseAll(LogParser.log, line).toDisjunction()
  
  def apply() = HttpService[IO] {
    case GET -> Root / "log" => {
      val log = RrdFile.path / "temperature-machine.log"
      
      val messages = for {
        scanner  <- fromTryCatchNonFatal(new Scanner(log).useDelimiter("\u0000")).leftMap(FileError)
        lines    <- fromTryCatchNonFatal(scanner.asScala.filterNot(_.trim.isEmpty)).leftMap(FileError)
        messages <- lines.map(toLogMessage).toList.sequenceU
        _        <- \/-(scanner.close())
      } yield messages

      messages.toHttpResponse(logs => Ok(logs))
    }
  }

}