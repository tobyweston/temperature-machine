package bad.robot.temperature.server

import bad.robot.temperature.rrd.{RrdFile, _}
import bad.robot.temperature.{Error, FileError, LogMessage, LogParser}
import org.http4s.HttpService
import org.http4s.dsl.{->, /, GET, Ok, Root, _}

import scala.io.Source
import scala.{Error => _}
import scalaz.Scalaz._
import scalaz.\/
import scalaz.\/.{fromTryCatchNonFatal, _}

object LogEndpoint {
  
  private val toLogMessage: String => Error \/ LogMessage = line => LogParser.parseAll(LogParser.log, line).toDisjunction()
  
  def service() = HttpService {
    case GET -> Root / "log" => {
      val log = RrdFile.path / "temperature-machine.log"
      
      val messages = for {
        lines    <- fromTryCatchNonFatal(Source.fromFile(log).getLines()).leftMap(FileError)
        messages <- lines.filterNot(_.trim.isEmpty).map(toLogMessage).toList.sequenceU
      } yield messages

      messages.toHttpResponse(Ok(_))
    }
  }

}