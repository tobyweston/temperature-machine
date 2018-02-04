package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature.rrd.{Host, Seconds}
import bad.robot.temperature.{Measurement, SensorReading, Temperature, UnexpectedError, jsonEncoder}
import cats.data.Kleisli
import cats.effect.IO
import org.http4s.Method.PUT
import org.http4s.client.{DisposableResponse, Client => Http4sClient}
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, Request}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

class HttpUploadTest extends Specification {

  "Ip address" >> {
    HttpUpload.currentIpAddress.size must be_>(0)
  }
  
  "Encode a measurement for the wire" >> {
    def encodeMessageViaEntityEncoder(measurement: Measurement): String = {
      implicit val encoder = jsonEncoder[Measurement]
      val request: IO[Request[IO]] = Request(PUT).withBody(measurement)
      EntityDecoder.decodeString(request.unsafeRunSync()).unsafeRunSync()
    }

    val measurement = Measurement(Host("example"), Seconds(1509221361), List(SensorReading("28-0115910f5eff", Temperature(19.75))))
    encodeMessageViaEntityEncoder(measurement) must_== """|{
                                                          |  "host" : "example",
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
    
    val error = InternalServerError("I'm an error").map(DisposableResponse(_, IO.pure(())))
    val willError: Kleisli[IO, Request[IO], DisposableResponse[IO]] = new Kleisli[IO, Request[IO], DisposableResponse[IO]](_ => error)
    
    val client = Http4sClient[IO](willError, IO.pure(()))

    val upload = HttpUpload(InetAddress.getLoopbackAddress, client)
    val value1 = upload.write(measurement)
    value1 must be_-\/.like {
      case UnexpectedError("""Failed to PUT temperature data to http://127.0.0.1:11900/temperature, response was 500 Internal Server Error: Right(I'm an error)""") => ok
    }
  }
}
