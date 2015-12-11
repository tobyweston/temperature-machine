package bad.robot.temperature.ds18b20

import bad.robot.temperature.{SensorError, Temperature}
import org.specs2.mutable.Specification
import org.specs2.matcher.DisjunctionMatchers._
import scala.{Error => _}

class ParserTest extends Specification {

  "Fails to extract temperature with failed CRC check" >> {
    val output =
      """
        |72 01 4b 46 7f ff 0e 10 57 : crc=57 NO
        |72 01 4b 46 7f ff 0e 10 57 t=23125
      """.stripMargin
    Parser.parse("") must be_-\/.like {
      case error: SensorError => error.message must contain("...")
    }
  }

  "Extract temperature from sensor output" >> {
    val output =
      """
        |a3 01 4b 46 7f ff 0e 10 d8 : crc=d8 YES
        |a3 01 4b 46 7f ff 0e 10 d8 t=32768
      """.stripMargin
    Parser.parse("") must be_\/-(Temperature(22.2))
  }

}
