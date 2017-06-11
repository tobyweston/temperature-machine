package bad.robot.temperature.ds18b20

import bad.robot.temperature.{CrcFailure, Temperature, UnexpectedError}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

import scala.{Error => _}

class ParserTest extends Specification {

  "Fails to extract temperature with failed CRC check" >> {
    val output =
      """|72 01 4b 46 7f ff 0e 10 57 : crc=57 NO
         |72 01 4b 46 7f ff 0e 10 57 t=23125
      """.stripMargin
    Parser.parse(output) must be_-\/.like {
      case error: CrcFailure => ok
    }
  }

  "Extract temperature from sensor output" >> {
    "Example 1" >> {
      val output =
        """|a3 01 4b 46 7f ff 0e 10 d8 : crc=d8 YES
           |a3 01 4b 46 7f ff 0e 10 d8 t=26187
        """.stripMargin
      Parser.parse(output) must be_\/-(Temperature(26.1875))
    }

    "Example 2" >> {
      val output =
        """|51 01 4b 46 7f ff 0c 10 ab : crc=ab YES
           |51 01 4b 46 7f ff 0c 10 ab t=21062
        """.stripMargin
      Parser.parse(output) must be_\/-(Temperature(21.0625))
    }
  }

  "Crazy strings don't compute" >> {
    Parser.parse("af  fdsajhkl luasdfhjklf  asdf") must be_-\/.like {
      case error: UnexpectedError => ok
    }
  }

  "Low temperature" >> {
    val output =
      """|64 00 4b 46 7f ff 0c 10 3c : crc=3c YES
         |64 00 4b 46 7f ff 0c 10 3c t=6250
      """.stripMargin
    Parser.parse(output) must be_\/-(Temperature(6.25))
  }

  "Negative temperatures (showing extra precision)" >> {
    val output =
      """|e1 ff 4b 46 7f ff 0c 10 fe : crc=fe YES
         |e1 ff 4b 46 7f ff 0c 10 fe t=-1937
      """.stripMargin
    Parser.parse(output) must be_\/-(Temperature(-1.9375))
  }

  "Negative temperatures" >> {
    "Example 1" >> {
      val output =
        """|7e ff 4b 46 7f ff 0c 10 c2 : crc=c2 YES
           |7e ff 4b 46 7f ff 0c 10 c2 t=-8125
        """.stripMargin
      Parser.parse(output) must be_\/-(Temperature(-8.125))
    }

    "Example 2" >> {
      val output =
        """|53 ff 4b 46 7f ff 0c 10 16 : crc=16 YES
           |53 ff 4b 46 7f ff 0c 10 16 t=-10812
        """.stripMargin
      Parser.parse(output) must be_\/-(Temperature(-10.8125))
    }
  }

  // https://cdn-shop.adafruit.com/datasheets/DS18B20.pdf
  "Various values from the spec sheet" >> {
    Parser.parse(scratchPadWith("d0", "07")) must be_\/-(Temperature(125.0))
    Parser.parse(scratchPadWith("50", "05")) must be_\/-(Temperature(85.0))
    Parser.parse(scratchPadWith("91", "01")) must be_\/-(Temperature(25.0625))
    Parser.parse(scratchPadWith("a2", "00")) must be_\/-(Temperature(10.125))
    Parser.parse(scratchPadWith("08", "00")) must be_\/-(Temperature(0.5))
    Parser.parse(scratchPadWith("00", "00")) must be_\/-(Temperature(0.0))
    Parser.parse(scratchPadWith("f8", "ff")) must be_\/-(Temperature(-0.5))
    Parser.parse(scratchPadWith("5e", "ff")) must be_\/-(Temperature(-10.125))
    Parser.parse(scratchPadWith("6f", "fe")) must be_\/-(Temperature(-25.0625))
    Parser.parse(scratchPadWith("90", "fc")) must be_\/-(Temperature(-55.0))
  }

  private def scratchPadWith(lsb: String, msb: String) = {
    s"""|$lsb $msb 4b 46 7f ff 0c 10 fe : crc=fe YES
        |$lsb $msb 4b 46 7f ff 0c 10 fe t=0
     """.stripMargin
  }

}
