package bad.robot.temperature.server

import java.io.{BufferedWriter, FileWriter}

import org.specs2.matcher.DisjunctionMatchers.be_\/-
import org.specs2.mutable.Specification

class JsonFileTest extends Specification {

  val exampleJson =
    """
      |[
      |  {
      |    "label": "bedroom1-sensor-1",
      |    "data": [
      |      {
      |        "x": 1507709610000,
      |        "y": "NaN"
      |      },
      |      {
      |        "x": 1507709640000,
      |        "y": "+2.2062500000E01"
      |      },
      |      {
      |        "x": 1507709680000,
      |        "y": "+2.2262500000E01"
      |      }
      |    ]
      |  }
      |]
    """.stripMargin

  "Load a file" >> {
    createFile()
    JsonFile.load must be_\/-(exampleJson)
  }
  
  private def createFile() = {
    val writer = new BufferedWriter(new FileWriter(JsonFile.file))
    writer.write(exampleJson)
    writer.close()
  }

}
