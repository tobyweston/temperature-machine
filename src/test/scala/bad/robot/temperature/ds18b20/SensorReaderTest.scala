package bad.robot.temperature.ds18b20

import org.specs2.mutable.Specification
import org.specs2.matcher.DisjunctionMatchers._

class SensorReaderTest extends Specification {

  "Read a value" >> {
    val file = SensorFile.find("src/test/resources/examples").head
    SensorReader(file).read must be_\/-
  }

}
