package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature.rrd.{Host, Seconds}
import bad.robot.temperature.{IpAddress, Measurement, SensorReading, Temperature, UnexpectedError, jsonEncoder}
import cats.effect.{ContextShift, IO, Resource, Timer}
import org.http4s.Method.PUT
import org.http4s.client.{Client => Http4sClient}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, HttpRoutes, Request}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global

class HttpUploadTest extends Specification {
  
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  private implicit val timer: Timer[IO] = IO.timer(global)

  "Ip address pre-check" >> {
    IpAddress.currentIpAddress.size must be_>(0)
  }
  
  "Encode a measurement for the wire" >> {
    def encodeMessageViaEntityEncoder(measurement: Measurement): String = {
      implicit val encoder = jsonEncoder[Measurement]
      val request: IO[Request[IO]] = Request(PUT).withBody(measurement)
      EntityDecoder.decodeString(request.unsafeRunSync()).unsafeRunSync()
    }

    val measurement = Measurement(Host("example"), Seconds(1509221361), List(SensorReading("28-0115910f5eff", Temperature(19.75))))
    encodeMessageViaEntityEncoder(measurement) must_== """|{
                                                          |  "host" : {
                                                          |    "name" : "example",
                                                          |    "utcOffset" : null,
                                                          |    "timezone" : null
                                                          |  },
                                                          |  "seconds" : 1509221361,
                                                          |  "sensors" : [
                                                          |    {
                                                          |      "name" : "28-0115910f5eff",
                                                          |      "temperature" : {
                                                          |        "celsius" : 19.75
                                                          |      }
                                                          |    }
                                                          |  ]
                                                          |}""".stripMargin
  }
  
  "Error response from server" >> {
    val measurement = Measurement(Host("example"), Seconds(1509221361), List(SensorReading("28-0115910f5eff", Temperature(19.75))))
    
    val client = Http4sClient.fromHttpApp(HttpRoutes.of[IO] {
      case _ => {
        InternalServerError("I'm an error")
      }
    }.orNotFound)

    
    val upload = HttpUpload(InetAddress.getLoopbackAddress, Resource.liftF(IO(client)))
    val value = upload.write(measurement)
    value must be_-\/.like {
      case UnexpectedError("""Failed to PUT temperature data to http://127.0.0.1:11900/temperature, response was 500 Internal Server Error: Right(I'm an error)""") => ok
    }
  }
  
  "Request has headers" >> {
    val measurement = Measurement(Host("example"), Seconds(1509221361), List(SensorReading("28-0115910f5eff", Temperature(19.75))))
    
    var headers = List[String]()
    
    val client = Http4sClient.fromHttpApp(HttpRoutes.of[IO] {
      case request => {
        headers = request.headers.toList.map(_.name.toString())
        Ok()
      }
    }.orNotFound)
    
    val upload = HttpUpload(InetAddress.getLoopbackAddress, Resource.liftF(IO(client)))
    upload.write(measurement)
    
    headers must_== List(
      "Content-Type",
      "X-Forwarded-For",
      "Content-Length",
      "Host"
    )
  }
}
