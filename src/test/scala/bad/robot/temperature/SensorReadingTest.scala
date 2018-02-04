package bad.robot.temperature

import org.specs2.mutable.Specification

class SensorReadingTest extends Specification {

  "Averages" >> {
    val a = SensorReading("A", Temperature(23.4))
    val b = SensorReading("B", Temperature(23.5))
    val c = SensorReading("C", Temperature(23.1))

    List(a, b, c).average must_== SensorReading("Average", Temperature(23.333333333333332))
  }

  "Average a single temperature" >> {
    val a = SensorReading("A", Temperature(23.4))

    List(a).average must_== SensorReading("A", Temperature(23.4))

  }

  "Averages an empty list" >> {
    List[SensorReading]().average must_== SensorReading("Unknown", Temperature(0.0))
  }

  "Encode JSON" >> {
    encode(SensorReading("A", Temperature(23.4))).spaces2ps must_==
      """{
        |  "name" : "A",
        |  "temperature" : {
        |    "celsius" : 23.4
        |  }
        |}""".stripMargin
  }
}
