package bad.robot.temperature.ds18b20

import java.io.File

import org.specs2.mutable.Specification

class SensorFileTest extends Specification {

  "Find some sensor files" >> {
    SensorFile.find("src/test/resources/examples") must_== List(
      new File("src/test/resources/examples/28-000005e2fdc2/w1_slave"),
      new File("src/test/resources/examples/28-000005e2fdc3/w1_slave")
    )
  }

  "Non existent folder doesn't barf" >> {
    SensorFile.find("nonexistent") must_== List()
  }
}
