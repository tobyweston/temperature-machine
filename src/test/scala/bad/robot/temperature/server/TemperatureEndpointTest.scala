package bad.robot.temperature.server

import bad.robot.temperature._
import bad.robot.temperature.server.Requests._
import bad.robot.temperature.test._
import org.http4s.Method.{GET, PUT}
import org.http4s.Status.{InternalServerError, NoContent, Ok}
import org.http4s.dsl._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification

import scalaz.{-\/, \/, \/-}

object Requests {
  val Put: String => Request = (body) => Request(PUT, Uri(path = s"temperature")).withBody(body).run
}

class TemperatureEndpointTest extends Specification {

  sequential

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
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), stubWriter(\/-(Unit)))
    val response = service.apply(Put("""{ "host" : "localhost", "seconds" : 9000, "sensors" : [ { "celsius" : 32.1 } ]}""")).run
    response must haveStatus(NoContent)
  }

  "Putting sensor data to the writer" >> {
    val body = """{ "host" : "localhost", "seconds" : 7000, "sensors" : [ { "celsius" : 31.1 }, { "celsius" : 32.8 } ]}"""
    val request = Request(PUT, Uri(path = s"temperature")).withBody(body).run
    var temperatures = List[Temperature]()
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), new TemperatureWriter {
      def write(measurement: Measurement) : \/[Error, Unit] = {
        temperatures = measurement.temperatures
        \/-(Unit)
      }
    })
    service.apply(request).run
    temperatures must_== List(Temperature(31.1), Temperature(32.8))
  }

  "Bad json when writing sensor data" >> {
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), stubWriter(\/-(Unit)))
    val response = service.apply(Put("bad json")).run
    response must haveStatus(BadRequest)
    response.as[String].run must_== "Unable to parse content as JSON Unexpected content found: bad json"
  }

  "Get multiple sensors, averaging the temperatures" >> {
    val service = TemperatureEndpoint.service(stubReader(\/-(List())), stubWriter(\/-(Unit)))
    service.apply(Request(DELETE, Uri.uri("/temperatures"))).run
    service.apply(Put("""{ "host" : "lounge", "seconds" : 100, "sensors" : [ { "celsius" : 31.1 }, { "celsius" : 32.8 } ]}""")).run
    service.apply(Put("""{ "host" : "bedroom", "seconds" : 200, "sensors" : [ { "celsius" : 21.1 }, { "celsius" : 22.8 } ]}""")).run

    val request = Request(GET, Uri.uri("/temperatures"))
    val response = service(request).run

    response.status must_== Ok
    response.as[String].run must_== """{
                                      |  "measurements" : [
                                      |    {
                                      |      "host" : "lounge",
                                      |      "seconds" : 100,
                                      |      "sensors" : [
                                      |        {
                                      |          "celsius" : 31.95
                                      |        }
                                      |      ]
                                      |    },
                                      |    {
                                      |      "host" : "bedroom",
                                      |      "seconds" : 200,
                                      |      "sensors" : [
                                      |        {
                                      |          "celsius" : 21.950000000000003
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin
  }

  def stubReader(result: Error \/ List[Temperature]) = new TemperatureReader {
    def read: Error \/ List[Temperature] = result
  }

  def stubWriter(result: Error \/ Unit) = new TemperatureWriter {
    def write(measurement: Measurement) = result
  }

  def UnexpectedWriter = new TemperatureWriter {
    def write(measurement: Measurement) = ???
  }

}