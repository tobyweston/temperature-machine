package bad.robot.temperature.server

import bad.robot.temperature.JsonToCsv
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
object ExportEndpoint {
  
  def apply() = HttpService[IO] {
    case GET -> Root / "temperatures" / "csv" => {
      val exampleJson =
        """
          |[
          |  {
          |    "label": "bedroom1-sensor-1",
          |    "data": [
          |      {
          |        "x": 1507709610000,
          |        "y": "NaN"
          |      },
          |      {
          |        "x": 1507709640000,
          |        "y": "+2.2062500000E01"
          |      },
          |      {
          |        "x": 1507709680000,
          |        "y": "+2.2262500000E01"
          |      }
          |    ]
          |  }
          |]
        """.stripMargin

      val csv = JsonToCsv.convert(exampleJson)
      csv.toHttpResponse(Ok(_))
    }

  }

}