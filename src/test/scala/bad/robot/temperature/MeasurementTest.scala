package bad.robot.temperature

import bad.robot.temperature.rrd.{Host, Seconds}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

class MeasurementTest extends Specification {

  "Encode json" >> {
    val expected = """{
                     |  "host" : "localhost",
                     |  "seconds" : 1000,
                     |  "sensors" : [
                     |    {
                     |      "name" : "28-00000f33fdc3",
                     |      "temperature" : {
                     |        "celsius" : 32.1
                     |      }
                     |    },
                     |    {
                     |      "name" : "28-00000dfg34ca",
                     |      "temperature" : {
                     |        "celsius" : 32.8
                     |      }
                     |    }
                     |  ]
                     |}""".stripMargin
    val json = encode(Measurement(Host("localhost"), Seconds(1000), List(
      SensorReading("28-00000f33fdc3", Temperature(32.1)),
      SensorReading("28-00000dfg34ca", Temperature(32.8)))
    )).spaces2ps
    json must_== expected
  }

  "Decode json" >> {
    val json = """{
                 |  "host" : "localhost",
                 |  "seconds" : 1000,
                 |  "sensors" : [
                 |     {
                 |        "name" : "28-00000dfg34ca",
                 |        "temperature" : {
                 |          "celsius" : 32.1
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
    decode[Measurement](json) must be_\/-(Measurement(Host("localhost"), Seconds(1000), List(
      SensorReading("28-00000dfg34ca", Temperature(32.1)),
      SensorReading("28-00000f33fdc3", Temperature(32.8)))
    ))
  }

}
