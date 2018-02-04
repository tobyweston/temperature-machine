package bad.robot.temperature.server

import java.time.format.DateTimeFormatter

import bad.robot.temperature.Error
import bad.robot.temperature.JsonToCsv
import cats.effect.IOimport org.http4s._
import org.http4s.MediaType._
importorg.http4s.dsl.io._
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}

import scalaz.\/
object ExportEndpoint {
  
  def apply(json: => Error \/ String, formatter: DateTimeFormatter) = HttpService[IO] {
    
    case GET -> Root / "temperatures.csv" => {
      val csv = JsonToCsv.convert(json, formatter)
      csv.toHttpResponse(Ok(_).putHeaders(
        `Content-Type`(`text/csv`),
        `Content-Disposition`("attachment", Map("filename" -> "temperatures.csv"))
      ))
    }

  }

}