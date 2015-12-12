package bad.robot.temperature.ds18b20

import bad.robot.temperature.{UnexpectedError, CrcFailure, Temperature}
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
    val output =
      """|a3 01 4b 46 7f ff 0e 10 d8 : crc=d8 YES
         |a3 01 4b 46 7f ff 0e 10 d8 t=26187
      """.stripMargin
    Parser.parse(output) must be_\/-(Temperature(26.1875))
  }

  "Crazy strings don't compute" >> {
    Parser.parse("af  fdsajhkl luasdfhjklf  asdf") must be_-\/.like {
      case error: UnexpectedError => ok
    }
  }

}
