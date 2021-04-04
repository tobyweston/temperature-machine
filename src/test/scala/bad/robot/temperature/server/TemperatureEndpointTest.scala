package bad.robot.temperature.server

import bad.robot.temperature._
import bad.robot.temperature.rrd.{Host, Seconds}
import bad.robot.temperature.server.Requests._
import bad.robot.temperature.test._
import cats.effect.{ContextShift, IO, Timer}
import org.http4s.Method._
import org.http4s.Status.{NoContent, Ok}
import org.http4s.implicits._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification
import scalaz.{\/, \/-}

import scala.concurrent.ExecutionContext.Implicits.global

object Requests {
  val Put: String => Request[IO] = (body) => Request[IO](PUT, Uri(path = s"temperature")).withBody(body).unsafeRunSync
}

class TemperatureEndpointTest extends Specification {

  private implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  private implicit val timer: Timer[IO] = IO.timer(global)


  sequential

  "Put some temperature data" >> {
    val service = TemperatureEndpoint(stubReader(\/-(List())), AllTemperatures(), Connections())
    val measurement = """{
                         |  "host" : {
                         |    "name" : "localhost",
                         |    "utcOffset" : null,
                         |    "timezone" : null
                         |  },
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
    val response = service.orNotFound.run(Put(measurement)).unsafeRunSync
    response must haveStatus(NoContent)
  }

  "Bad json when writing sensor data" >> {
    val service = TemperatureEndpoint(stubReader(\/-(List())), AllTemperatures(), Connections())
    val request: Request[IO] = Put("bad json")
    val response = service.orNotFound.run(request).unsafeRunSync
    response must haveStatus(org.http4s.Status.BadRequest)
    response.as[String].unsafeRunSync must_== "The request body was malformed."
  }

  "Get multiple sensors temperatures" >> {
    val now = Seconds.now()
    val measurement1 = s"""{
                         |  "host" : {
                         |    "name" : "lounge",
                         |    "utcOffset" : null,
                         |    "timezone" : null
                         |  },
                         |  "seconds" : ${now.value - 10},
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

    val measurement2 = s"""{
                         |  "host" : {
                         |    "name" : "bedroom",
                         |    "utcOffset" : null,
                         |    "timezone" : null
                         |  },
                         |  "seconds" : ${now.value},
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


    val service = TemperatureEndpoint(stubReader(\/-(List())), AllTemperatures(), Connections())
    service.orNotFound.run(Request[IO](DELETE, Uri.uri("/temperatures"))).unsafeRunSync
    service.orNotFound.run(Put(measurement1)).unsafeRunSync
    service.orNotFound.run(Put(measurement2)).unsafeRunSync

    val request = Request[IO](GET, Uri.uri("/temperatures"))
    val response = service.orNotFound.run(request).unsafeRunSync

    response.status must_== Ok

    val expected = s"""{
                      |  "measurements" : [
                      |    {
                      |      "host" : {
                      |        "name" : "lounge",
                      |        "utcOffset" : null,
                      |        "timezone" : null
                      |      },
                      |      "seconds" : ${now.value - 10},
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
                      |      "host" : {
                      |        "name" : "bedroom",
                      |        "utcOffset" : null,
                      |        "timezone" : null
                      |      },
                      |      "seconds" : ${now.value},
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
    val now = Seconds.now()
    val measurement1 = s"""{
                         |  "host" : {
                         |    "name" : "lounge",
                         |    "utcOffset" : null,
                         |    "timezone" : null
                         |  },
                         |  "seconds" : ${now.value - 10},
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

    val measurement2 = s"""{
                         |  "host" : {
                         |    "name" : "bedroom",
                         |    "utcOffset" : null,
                         |    "timezone" : null
                         |  },
                         |  "seconds" : ${now.value},
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


    val service = TemperatureEndpoint(stubReader(\/-(List())), AllTemperatures(), Connections())
    service.orNotFound.run(Request[IO](DELETE, Uri.uri("/temperatures"))).unsafeRunSync
    service.orNotFound.run(Put(measurement1)).unsafeRunSync
    service.orNotFound.run(Put(measurement2)).unsafeRunSync

    val request = Request[IO](GET, Uri.uri("/temperatures/average"))
    val response = service.orNotFound.run(request).unsafeRunSync

    response.status must_== Ok
    response.as[String].unsafeRunSync must_==
                                    s"""{
                                      |  "measurements" : [
                                      |    {
                                      |      "host" : {
                                      |        "name" : "lounge",
                                      |        "utcOffset" : null,
                                      |        "timezone" : null
                                      |      },
                                      |      "seconds" : ${now.value - 10},
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
                                      |      "host" : {
                                      |        "name" : "bedroom",
                                      |        "utcOffset" : null,
                                      |        "timezone" : null
                                      |      },
                                      |      "seconds" : ${now.value},
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
    def read: Error \/ Measurement = result.map(x => Measurement(Host("A"), Seconds.now(), x))
  }

  def stubWriter(result: Error \/ Unit) = new TemperatureWriter {
    def write(measurement: Measurement) = result
  }

  def UnexpectedWriter = new TemperatureWriter {
    def write(measurement: Measurement) = ???
  }
}