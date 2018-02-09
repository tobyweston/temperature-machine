package bad.robot.temperature

import bad.robot.temperature.rrd.{Host, Seconds}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

class MeasurementTest extends Specification {

  "Encode json" >> {
    val expected = """{
                     |  "host" : {
                     |    "name" : "localhost",
                     |    "utcOffset" : null
                     |  },
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
    val json = encode(Measurement(Host("localhost", None), Seconds(1000), List(
      SensorReading("28-00000f33fdc3", Temperature(32.1)),
      SensorReading("28-00000dfg34ca", Temperature(32.8)))
    )).spaces2ps
    json must_== expected
  }
  
//  "Decode json (pre v2.1 - supporting backwards compatibility)" >> {
//    val json = """{
//                 |  "host" : "localhost",
//                 |  "seconds" : 1000,
//                 |  "sensors" : [
//                 |     {
//                 |        "name" : "28-00000dfg34ca",
//                 |        "temperature" : {
//                 |          "celsius" : 32.1
//                 |        }
//                 |     },
//                 |     {
//                 |        "name" : "28-00000f33fdc3",
//                 |        "temperature" : {
//                 |          "celsius" : 32.8
//                 |       }
//                 |     }
//                 |   ]
//                 |}""".stripMargin
//    decodeAsDisjunction[Measurement](json) must be_\/-(Measurement(Host("localhost", None), Seconds(1000), List(
//      SensorReading("28-00000dfg34ca", Temperature(32.1)),
//      SensorReading("28-00000f33fdc3", Temperature(32.8)))
//    ))
//  }

  "Decode json (post v2.1)" >> {
    val json = """{
                 |  "host" : {
                 |    "name" : "localhost",
                 |    "utcOffset" : "+01:00"
                 |  },
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
    decodeAsDisjunction[Measurement](json) must be_\/-(Measurement(Host("localhost", Some("+01:00")), Seconds(1000), List(
      SensorReading("28-00000dfg34ca", Temperature(32.1)),
      SensorReading("28-00000f33fdc3", Temperature(32.8)))
    ))
  }

}
