package bad.robot.temperature

import org.specs2.mutable.Specification

class SensorTemperatureTest extends Specification {

  "Averages" >> {
    val a = SensorTemperature("A", Temperature(23.4))
    val b = SensorTemperature("B", Temperature(23.5))
    val c = SensorTemperature("C", Temperature(23.1))

    List(a, b, c).average must_== SensorTemperature("Average", Temperature(23.333333333333332))
  }

  "Averages an empty list" >> {
    List[SensorTemperature]().average must_== SensorTemperature("Unknown", Temperature(0.0))
  }
}
