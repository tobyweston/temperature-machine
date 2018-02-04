package bad.robot.temperature.server

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT
import java.util.Locale._

import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.implicits._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification

import scalaz.syntax.either._

class ExportEndpointTest extends Specification {

  sequential

  "convert json to csv" >> {
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
    
    val expectedCsv = """"Sensor","Time","Temperature","%Difference"
                        |"bedroom1-sensor-1","11/10/17 08:13","NaN","0.0"
                        |"bedroom1-sensor-1","11/10/17 08:14","22.0625","NaN"
                        |"bedroom1-sensor-1","11/10/17 08:14","22.2625","0.91"""".stripMargin

    val UkDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(SHORT).withLocale(UK).withZone(ZoneId.of("GMT"))
    
    val request = Request[IO](GET, Uri.uri("/temperatures.csv"))
    val service = ExportEndpoint(exampleJson.right, UkDateTimeFormatter)
    val response = service.orNotFound.run(request).unsafeRunSync()
    response.as[String].unsafeRunSync must_== expectedCsv
    response.status must_== Ok
  }

}