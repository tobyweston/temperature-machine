package bad.robot.temperature.server

import bad.robot.temperature.JsonToCsv
import org.http4s.HttpService
import org.http4s.dsl._

object ExportEndpoint {
  
  def apply() = HttpService {
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