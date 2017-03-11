package bad.robot.temperature.ds18b20

import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

import scalaz.concurrent.Task

class SensorFileTest extends Specification {

  "Find some sensor files" >> {
    SensorFile.find("src/test/resources/examples") must contain(exactly(
      new SensorFile("src/test/resources/examples/28-000005e2fdc2/w1_slave"),
      new SensorFile("src/test/resources/examples/28-000005e2fdc3/w1_slave")
    ))
  }

  "Non existent folder" >> {
    SensorFile.find("nonexistent") must_== List()
  }

  "Empty contents" >> {
    SensorFile.find("src/test") must_== List()
  }

  "Find some sensor files (using system property) and execute" >> {
    def example(sensors: List[SensorFile]) = Task.delay(sensors)

    sys.props += ("sensor.location" -> "src/test/resources/examples")

    val result = SensorFile.findSensorsAndExecute[List[SensorFile]](example)
    result must be_\/-(List(
      new SensorFile("src/test/resources/examples/28-000005e2fdc2/w1_slave"),
      new SensorFile("src/test/resources/examples/28-000005e2fdc3/w1_slave")
    ))
  }

  "Fail to find some sensor files (using system property) and not execute" >> {
    def example(sensors: List[SensorFile]) = Task.delay(sensors)

    sys.props += ("sensor.location" -> "missing")

    val result = SensorFile.findSensorsAndExecute(example)
    result must be_-\/
  }
}
