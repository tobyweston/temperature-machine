package bad.robot.temperature

import bad.robot.temperature.rrd.Seconds
import org.specs2.mutable.Specification
import org.specs2.matcher.DisjunctionMatchers._

class MeasurementTest extends Specification {

  "Encode json" >> {
    val expected = """{
                     |  "source" : "localhost",
                     |  "seconds" : 1000,
                     |  "sensors" : [
                     |    {
                     |      "celsius" : 32.1
                     |    },
                     |    {
                     |      "celsius" : 32.8
                     |    }
                     |  ]
                     |}""".stripMargin
    val json = encode(Measurement("localhost", Seconds(1000), List(Temperature(32.1), Temperature(32.8)))).spaces2
    json must_== expected
  }

  "Decode json" >> {
    val json = """{ "source" : "localhost", "seconds" : 1000, "sensors" : [ { "celsius" : 32.1 }, { "celsius" : 32.8 } ]}"""
    decode[Measurement](json) must be_\/-(Measurement("localhost", Seconds(1000), List(Temperature(32.1), Temperature(32.8))))
  }

}
