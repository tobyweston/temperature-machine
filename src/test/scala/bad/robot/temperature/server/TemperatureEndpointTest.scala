package bad.robot.temperature.server

import java.time.{Clock, Instant, ZoneId}

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
  val Put: String => Request = (body) => Request(PUT, Uri(path = s"temperature")).withBody(body).unsafeRunSync
}

class TemperatureEndpointTest extends Specification {

  sequential

  "Averages a single temperature" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val clock = fixedClock()
    val reader = stubReader(\/-(List(SensorReading("28-0002343fd", Temperature(56.34)))))
    val service = TemperatureEndpoint(reader, UnexpectedWriter)(clock)
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== Ok
    response.as[String].unsafeRunSync must_== "56.3 °C"
  }

  "Averages several temperatures" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val clock = fixedClock()
    val service = TemperatureEndpoint(stubReader(\/-(List(
      SensorReading("28-0000d34c3", Temperature(25.344)),
      SensorReading("28-0000d34c3", Temperature(23.364)),
      SensorReading("28-0000d34c3", Temperature(21.213))
    ))), UnexpectedWriter)(clock)
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== Ok
    response.as[String].unsafeRunSync must_== "23.3 °C"
  }

  "General Error reading temperatures" >> {
    val request = Request(GET, Uri.uri("/temperature"))
    val clock = fixedClock()
    val service = TemperatureEndpoint(stubReader(-\/(SensorError("An example error"))), UnexpectedWriter)(clock)
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== InternalServerError
    response.as[String].unsafeRunSync must_== "An example error"
  }

  "Put some temperature data" >> {
    val clock = fixedClock()
    val service = TemperatureEndpoint(stubReader(\/-(List())), stubWriter(\/-(Unit)))(clock)
    val measurement = """{
                         |  "host" : "localhost",
                         |  "seconds" : 9000,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000dfg34ca",
                         |        "temperature" : {
                         |          "celsius" : 31.1
                         |        }
                         |     }
                         |   ]
                         |}""".stripMargin
    val response = service.apply(Put(measurement)).unsafeRunSync.orNotFound
    response must haveStatus(NoContent)
  }

  "Putting sensor data to the writer" >> {
    val body = """{
                  |  "host" : "localhost",
                  |  "seconds" : 7000,
                  |  "sensors" : [
                  |     {
                  |        "name" : "28-00000dfg34ca",
                  |        "temperature" : {
                  |          "celsius" : 31.1
                  |        }
                  |     },
                  |     {
                  |        "name" : "28-0000012d432b",
                  |        "temperature" : {
                  |          "celsius" : 32.8
                  |        }
                  |     }
                  |   ]
                  |}""".stripMargin
    val request = Request(PUT, Uri(path = "temperature")).withBody(body).unsafeRunSync
    var temperatures = List[Temperature]()
    val clock = fixedClock()
    val service = TemperatureEndpoint(stubReader(\/-(List())), new TemperatureWriter {
      def write(measurement: Measurement) : \/[Error, Unit] = {
        temperatures = measurement.temperatures.map(_.temperature)
        \/-(Unit)
      }
    })(clock)
    service.apply(request).unsafeRunSync
    temperatures must_== List(Temperature(31.1), Temperature(32.8))
  }

  "Bad json when writing sensor data" >> {
    val clock = fixedClock()
    val service = TemperatureEndpoint(stubReader(\/-(List())), stubWriter(\/-(Unit)))(clock)
    val response = service.apply(Put("bad json")).unsafeRunSync.orNotFound
    response must haveStatus(BadRequest)
    response.as[String].unsafeRunSync must_== "Unable to parse content as JSON Unexpected content found: bad json"
  }

  "Get multiple sensors temperatures" >> {
    val measurement1 = """{
                         |  "host" : "lounge",
                         |  "seconds" : 100,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000dfg34ca",
                         |        "temperature" : {
                         |          "celsius" : 31.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-00000f33fdc3",
                         |        "temperature" : {
                         |          "celsius" : 32.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin

    val measurement2 = """{
                         |  "host" : "bedroom",
                         |  "seconds" : 200,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000f3554ds",
                         |        "temperature" : {
                         |          "celsius" : 21.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-000003dd3433",
                         |        "temperature" : {
                         |          "celsius" : 22.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin


    val clock = fixedClock(Instant.ofEpochSecond(200))
    val service = TemperatureEndpoint(stubReader(\/-(List())), stubWriter(\/-(Unit)))(clock)
    service.apply(Request(DELETE, Uri.uri("/temperatures"))).unsafeRunSync
    service.apply(Put(measurement1)).unsafeRunSync
    service.apply(Put(measurement2)).unsafeRunSync

    val request = Request(GET, Uri.uri("/temperatures"))
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== Ok

    val expected = """{
                      |  "measurements" : [
                      |    {
                      |      "host" : "lounge",
                      |      "seconds" : 100,
                      |      "sensors" : [
                      |        {
                      |          "name" : "28-00000dfg34ca",
                      |          "temperature" : {
                      |            "celsius" : 31.1
                      |          }
                      |        },
                      |        {
                      |          "name" : "28-00000f33fdc3",
                      |          "temperature" : {
                      |            "celsius" : 32.8
                      |          }
                      |        }
                      |      ]
                      |    },
                      |    {
                      |      "host" : "bedroom",
                      |      "seconds" : 200,
                      |      "sensors" : [
                      |        {
                      |          "name" : "28-00000f3554ds",
                      |          "temperature" : {
                      |            "celsius" : 21.1
                      |          }
                      |        },
                      |        {
                      |          "name" : "28-000003dd3433",
                      |          "temperature" : {
                      |            "celsius" : 22.8
                      |          }
                      |        }
                      |      ]
                      |    }
                      |  ]
                      |}""".stripMargin

    response.as[String].unsafeRunSync must_== expected
  }

  "Get multiple sensors, averaging the temperatures" >> {
    val measurement1 = """{
                         |  "host" : "lounge",
                         |  "seconds" : 100,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000dfg34ca",
                         |        "temperature" : {
                         |          "celsius" : 31.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-00000f33fdc3",
                         |        "temperature" : {
                         |          "celsius" : 32.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin

    val measurement2 = """{
                         |  "host" : "bedroom",
                         |  "seconds" : 200,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000f3554ds",
                         |        "temperature" : {
                         |          "celsius" : 21.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-000003dd3433",
                         |        "temperature" : {
                         |          "celsius" : 22.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin


    val clock = fixedClock(Instant.ofEpochSecond(200))
    val service = TemperatureEndpoint(stubReader(\/-(List())), stubWriter(\/-(Unit)))(clock)
    service.apply(Request(DELETE, Uri.uri("/temperatures"))).unsafeRunSync
    service.apply(Put(measurement1)).unsafeRunSync
    service.apply(Put(measurement2)).unsafeRunSync

    val request = Request(GET, Uri.uri("/temperatures/average"))
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== Ok
    response.as[String].unsafeRunSync must_==
                                    """{
                                      |  "measurements" : [
                                      |    {
                                      |      "host" : "lounge",
                                      |      "seconds" : 100,
                                      |      "sensors" : [
                                      |        {
                                      |          "name" : "Average",
                                      |          "temperature" : {
                                      |            "celsius" : 31.95
                                      |          }
                                      |        }
                                      |      ]
                                      |    },
                                      |    {
                                      |      "host" : "bedroom",
                                      |      "seconds" : 200,
                                      |      "sensors" : [
                                      |        {
                                      |          "name" : "Average",
                                      |          "temperature" : {
                                      |            "celsius" : 21.950000000000003
                                      |          }
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin
  }

  "Filter out old temperatures when getting multiple sensors" >> {
    val measurement1 = """{
                         |  "host" : "lounge",
                         |  "seconds" : 0,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000dfg34ca",
                         |        "temperature" : {
                         |          "celsius" : 31.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-00000f33fdc3",
                         |        "temperature" : {
                         |          "celsius" : 32.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin

    val measurement2 = """{
                         |  "host" : "bedroom",
                         |  "seconds" : 120,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000f3554ds",
                         |        "temperature" : {
                         |          "celsius" : 21.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-000003dd3433",
                         |        "temperature" : {
                         |          "celsius" : 22.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin


    val clock = fixedClock(Instant.ofEpochSecond(300))
    val service = TemperatureEndpoint(stubReader(\/-(List())), stubWriter(\/-(Unit)))(clock)
    service.apply(Request(DELETE, Uri.uri("/temperatures"))).unsafeRunSync
    service.apply(Put(measurement1)).unsafeRunSync
    service.apply(Put(measurement2)).unsafeRunSync

    val request = Request(GET, Uri.uri("/temperatures"))
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== Ok

    val expected = """{
                     |  "measurements" : [
                     |    {
                     |      "host" : "bedroom",
                     |      "seconds" : 120,
                     |      "sensors" : [
                     |        {
                     |          "name" : "28-00000f3554ds",
                     |          "temperature" : {
                     |            "celsius" : 21.1
                     |          }
                     |        },
                     |        {
                     |          "name" : "28-000003dd3433",
                     |          "temperature" : {
                     |            "celsius" : 22.8
                     |          }
                     |        }
                     |      ]
                     |    }
                     |  ]
                     |}""".stripMargin

    response.as[String].unsafeRunSync must_== expected
  }

  "Filter out old temperatures when averaging" >> {
    val measurement1 = """{
                         |  "host" : "lounge",
                         |  "seconds" : 0,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000dfg34ca",
                         |        "temperature" : {
                         |          "celsius" : 31.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-00000f33fdc3",
                         |        "temperature" : {
                         |          "celsius" : 32.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin

    val measurement2 = """{
                         |  "host" : "bedroom",
                         |  "seconds" : 60,
                         |  "sensors" : [
                         |     {
                         |        "name" : "28-00000f3554ds",
                         |        "temperature" : {
                         |          "celsius" : 21.1
                         |        }
                         |     },
                         |     {
                         |        "name" : "28-000003dd3433",
                         |        "temperature" : {
                         |          "celsius" : 22.8
                         |       }
                         |     }
                         |   ]
                         |}""".stripMargin


    val clock = fixedClock(Instant.ofEpochSecond(300))
    val service = TemperatureEndpoint(stubReader(\/-(List())), stubWriter(\/-(Unit)))(clock)
    service.apply(Request(DELETE, Uri.uri("/temperatures"))).unsafeRunSync
    service.apply(Put(measurement1)).unsafeRunSync
    service.apply(Put(measurement2)).unsafeRunSync

    val request = Request(GET, Uri.uri("/temperatures/average"))
    val response = service(request).unsafeRunSync.orNotFound

    response.status must_== Ok
    response.as[String].unsafeRunSync must_==
                                    """{
                                      |  "measurements" : [
                                      |    {
                                      |      "host" : "bedroom",
                                      |      "seconds" : 60,
                                      |      "sensors" : [
                                      |        {
                                      |          "name" : "Average",
                                      |          "temperature" : {
                                      |            "celsius" : 21.950000000000003
                                      |          }
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin
  }

  def stubReader(result: Error \/ List[SensorReading]) = new TemperatureReader {
    def read: Error \/ List[SensorReading] = result
  }

  def stubWriter(result: Error \/ Unit) = new TemperatureWriter {
    def write(measurement: Measurement) = result
  }

  def UnexpectedWriter = new TemperatureWriter {
    def write(measurement: Measurement) = ???
  }

  def fixedClock(instant: Instant = Instant.now) = Clock.fixed(instant, ZoneId.systemDefault())

}