package bad.robot.temperature.server

import java.time.format.DateTimeFormatter

import bad.robot.temperature.{Error, JsonToCsv}
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}

import scalaz.\/
import org.http4s.MediaType.text

object ExportEndpoint {

  def apply(json: => Error \/ String, formatter: DateTimeFormatter) = HttpRoutes.of[IO] {

    case GET -> Root / "temperatures.csv" => {
      val csv = JsonToCsv.convert(json, formatter)
      csv.toHttpResponse(Ok(_).map(_.putHeaders(
        `Content-Type`(text.csv),
        `Content-Disposition`("attachment", Map("filename" -> "temperatures.csv"))
      )))
    }

  }

}