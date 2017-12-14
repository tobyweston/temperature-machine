package bad.robot.temperature

import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

class TemperatureTest extends Specification {

  "Celsius" >> {
    Temperature(23.125).celsius must_== 23.125
  }

  "Fahrenheit" >> {
    Temperature(23.125).fahrenheit must_== 73.625
  }

  "Encode json" >> {
    val expected =
      """{
        |  "celsius" : 66.99
        |}""".stripMargin
    encode(Temperature(66.99)).spaces2ps must_== expected
  }

  "Decode json" >> {
    val temperature = """{ "celsius" : 99.1 }"""
    val result = decode[Temperature](temperature)
    result must be_\/-(Temperature(99.1))
  }

}
