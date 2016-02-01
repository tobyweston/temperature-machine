package bad.robot.temperature.server

import bad.robot.temperature._
import bad.robot.temperature.test._
import org.http4s.Method.{GET, PUT}
import org.http4s.Status.{InternalServerError, NoContent, Ok}
import org.http4s.dsl._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification

import scalaz.{-\/, \/, \/-}

class TemperatureEndpointTest extends Specification {

  "Averages a single temperature" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val service = TemperatureEndpoint.service(stubReader(\/-(List(Temperature(56.34)))), UnexpectedWriter)
    val response = service(request).run

    response.status must_== Ok
    response.as[String].run must_== "56.3 °C"
  }

  "Averages several temperatures" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val service = TemperatureEndpoint.service(stubReader(\/-(List(
      Temperature(25.344),
      Temperature(23.364),
      Temperature(21.213)
    ))), UnexpectedWriter)
    val response = service(request).run

    response.status must_== Ok
    response.as[String].run must_== "23.3 °C"
  }

  "General Error reading temperatures" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val service = TemperatureEndpoint.service(stubReader(-\/(SensorError("An example error"))), UnexpectedWriter)
    val response = service(request).run

    response.status must_== InternalServerError
    response.as[String].run must_== "An example error"
  }

  "Put some temperature data" >> {
    val body = """{ "source" : "localhost", "seconds" : 1000, "sensors" : [ { "celsius" : 32.1 } ]}"""
    val request = Request(PUT, Uri(path = s"temperature")).withBody(body).run
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), stubWriter(\/-(Unit)))
    val response = service.apply(request).run
    response must haveStatus(NoContent)
  }

  "Putting sensor data to the writer" >> {
    val body = """{ "source" : "localhost", "seconds" : 1000, "sensors" : [ { "celsius" : 31.1 }, { "celsius" : 32.8 } ]}"""
    val request = Request(PUT, Uri(path = s"temperature")).withBody(body).run
    var temperatures = List[Temperature]()
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), new TemperatureWriter {
      def write(data: List[Temperature]): \/[Error, Unit] = {
        temperatures = data
        \/-(Unit)
      }
    })
    service.apply(request).run
    temperatures must_== List(Temperature(31.1), Temperature(32.8))
  }

  "Bad json when writing sensor data" >> {
    val request = Request(PUT, Uri(path = s"temperature")).withBody("bad json").run
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), stubWriter(\/-(Unit)))
    val response = service.apply(request).run
    response must haveStatus(BadRequest)
    response.as[String].run must_== "Unable to parse content as JSON Unexpected content found: bad json"
  }

  def stubReader(result: Error \/ List[Temperature]) = new TemperatureReader {
    def read: Error \/ List[Temperature] = result
  }

  def stubWriter(result: Error \/ Unit) = new TemperatureWriter {
    def write(temperature: List[Temperature]) = result
  }

  def UnexpectedWriter = new TemperatureWriter {
    def write(temperature: List[Temperature]) = ???
  }

}