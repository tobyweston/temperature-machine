package bad.robot.temperature.client

import bad.robot.temperature.rrd.{Host, Seconds}
import bad.robot.temperature.{Measurement, SensorReading, Temperature}
import org.http4s.Method._
import org.http4s.{EntityDecoder, EntityEncoder, Request}
import org.specs2.mutable.Specification

class HttpUploadTest extends Specification {

  "Ip address" >> {
    HttpUpload.currentIpAddress.size must be_>(0)
  }
  
  "Encode a measurement for the wire" >> {

    def encodeMessageViaEntityEncoder[T](thing: T)(implicit encoder: EntityEncoder[T]) = {
      val request = Request(PUT).withBody(thing)
      EntityDecoder.decodeString(request.unsafeRunSync).unsafeRunSync
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
}
