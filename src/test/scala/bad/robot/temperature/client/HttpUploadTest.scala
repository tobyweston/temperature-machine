package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature.rrd.{Host, Seconds}
import bad.robot.temperature.{Measurement, SensorReading, Temperature, UnexpectedError}
import org.http4s.Method.PUT
import org.http4s.client.{DisposableResponse, Client => Http4sClient}
import org.http4s.dsl._
import org.http4s.{EntityDecoder, EntityEncoder, Request}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

import scalaz.Kleisli
import scalaz.concurrent.Task

class HttpUploadTest extends Specification {

  "Ip address" >> {
    HttpUpload.currentIpAddress.size must be_>(0)
  }
  
  "Encode a measurement for the wire" >> {

    def encodeMessageViaEntityEncoder[T](thing: T)(implicit encoder: EntityEncoder[T]) = {
      val request = Request(PUT).withBody(thing)
      EntityDecoder.decodeString(request.unsafePerformSync).unsafePerformSync
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
    val willError: Request => Task[DisposableResponse] = _ => InternalServerError().map(DisposableResponse(_, Task.delay(())))
    val client = Http4sClient(new Kleisli[Task, Request, DisposableResponse](willError), Task.delay(()))

    val upload = HttpUpload(InetAddress.getLoopbackAddress, client)
    upload.write(measurement) must be_-\/.like {
      case UnexpectedError("Failed to PUT temperature data to http://127.0.0.1:11900/temperature, response was 500 Internal Server Error: -\\/(org.http4s.MalformedMessageBodyFailure: Malformed message body: Invalid JSON: empty body)") => ok
    }
  }
}
