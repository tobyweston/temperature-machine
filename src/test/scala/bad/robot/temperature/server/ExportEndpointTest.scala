package bad.robot.temperature.server

import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.dsl._
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
                        |"bedroom1-sensor-1","11/10/17 09:13","NaN","0.0"
                        |"bedroom1-sensor-1","11/10/17 09:14","22.0625","NaN"
                        |"bedroom1-sensor-1","11/10/17 09:14","22.2625","0.91"""".stripMargin
    
    val request = Request(GET, Uri.uri("/temperatures.csv"))
    val service = ExportEndpoint(exampleJson.right)
    val response = service(request).unsafePerformSync.orNotFound
    response.as[String].unsafePerformSync must_== expectedCsv
    response.status must_== Ok
  }

}