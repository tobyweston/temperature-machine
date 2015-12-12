package bad.robot.temperature

import org.specs2.mutable.Specification

class TemperatureTest extends Specification {

  "Celsius" >> {
    Temperature(23.125).celsius must_== 23.125
  }

  "Fahrenheit" >> {
    Temperature(23.125).fahrenheit must_== 73.625
  }

}
