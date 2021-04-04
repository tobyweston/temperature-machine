package bad.robot.temperature.server

import java.util.Scanner

import bad.robot.logging.LogFile
import bad.robot.temperature.{Error, FileError, LogMessage, LogParser, _}
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import scalaz.Scalaz._
import scalaz.\/.{fromTryCatchNonFatal, _}
import scalaz.{\/, \/-}

import scala.collection.JavaConverters._


object LogEndpoint {
  
  private val toLogMessage: String => Error \/ LogMessage = line => LogParser.parseAll(LogParser.log, line).toDisjunction()

  private implicit val encoder = jsonEncoder[List[LogMessage]]

  def apply() = HttpRoutes.of[IO] {

    case GET -> Root / "log" => {
      val messages = for {
        scanner  <- fromTryCatchNonFatal(new Scanner(LogFile.file).useDelimiter("\u0000")).leftMap(FileError)
        lines    <- fromTryCatchNonFatal(scanner.asScala.filterNot(_.trim.isEmpty)).leftMap(FileError)
        messages <- lines.map(toLogMessage).toList.sequenceU
        _        <- \/-(scanner.close())
      } yield messages

      messages.toHttpResponse(logs => Ok.apply(logs))
    }
  }

}