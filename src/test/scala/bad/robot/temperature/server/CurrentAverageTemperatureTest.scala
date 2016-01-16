package bad.robot.temperature.server

import bad.robot.temperature._
import org.http4s.Method._
import org.http4s.Status._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification

import scalaz.{-\/, \/, \/-}

class CurrentAverageTemperatureTest extends Specification {

  "Averages a single temperature" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val service = CurrentAverageTemperature.service(stub(\/-(List(Temperature(56.34)))))
    val response = service(request).run

    response.status must_== Ok
    response.as[String].run must_== "56.3 °C"
  }

  "Averages several temperatures" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val service = CurrentAverageTemperature.service(stub(\/-(List(
      Temperature(25.344),
      Temperature(23.364),
      Temperature(21.213)
    ))))
    val response = service(request).run

    response.status must_== Ok
    response.as[String].run must_== "23.3 °C"
  }

  "General Error reading temperatures" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val service = CurrentAverageTemperature.service(stub(-\/(SensorError("An example error"))))
    val response = service(request).run

    response.status must_== InternalServerError
    response.as[String].run must_== "An example error"
  }

  def stub(result: Error \/ List[Temperature]) = new TemperatureReader {
    def read: Error \/ List[Temperature] = result
  }

}
