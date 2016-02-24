package bad.robot.temperature.rrd

import java.io.File

import org.rrd4j.DsType.GAUGE
import org.rrd4j.core.{DsDef, RrdDb, RrdDef}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

import scala.Double._

class RrdDbOpsTest extends Specification with BeforeEach {
  sequential

  def before = DataSources.updated = Set[String]()


  val frequency = Seconds(30)

  "Rrd file with no values" >> {
    val file = File.createTempFile("test", ".rrd")
    createRrdFile(createDefinition(file, "example"))

    val database = new RrdDb(file)
    database.hasValuesFor("example") must_== false
  }

  "Attempting to check values for unknown datasource" >> {
    val file = File.createTempFile("test", ".rrd")
    createRrdFile(createDefinition(file, "example"))

    val database = new RrdDb(file)
    database.hasValuesFor("doesnt_exist") must throwA(new IllegalArgumentException("Unknown datasource name: doesnt_exist"))
  }

  "Partially populated archive passes the 'hasValuesFor' check" >> {
    val file = File.createTempFile("test", ".rrd")
    createRrdFile(createDefinition(file, "example"))

    update(file, Seconds( 5), 1.0)
    update(file, Seconds(35), 2.0)
    update(file, Seconds(65), NaN)

    val database = new RrdDb(file)
    database.hasValuesFor("example").aka("the 'hasValuesFor' result for the 'example' datasource") must_== true
  }

  "Partially populated, multiple datasource archive passes the 'hasValuesFor' check" >> {
    val file = File.createTempFile("test", ".rrd")
    createRrdFile(createDefinition(file, "example-1", "example-2"))

    update(file, Seconds( 5), 1.0, NaN)
    update(file, Seconds(35), NaN, 2.0)
    update(file, Seconds(60), NaN, NaN)

    val database = new RrdDb(file)
    database.hasValuesFor("example-1").aka("the 'hasValuesFor' result for the 'example-1' datasource") must_== true
    database.hasValuesFor("example-2").aka("the 'hasValuesFor' result for the 'example-2' datasource") must_== true
  }

  "More complex example of partially populated, multiple datasource archive" >> {
    val file = File.createTempFile("test", ".rrd")
    createRrdFile(createDefinition(file, "example-1", "example-2", "example-3", "example-4"))

    update(file, Seconds( 5), 1.0, NaN, NaN, NaN)
    update(file, Seconds(35), NaN, NaN, 2.0, NaN)
    update(file, Seconds(65), NaN, NaN, NaN, NaN)
    update(file, Seconds(95), NaN, NaN, NaN, NaN)

    val database = new RrdDb(file)
    database.hasValuesFor("example-1").aka("the 'hasValuesFor' result for the 'example-1' datasource") must_== true
    database.hasValuesFor("example-2").aka("the 'hasValuesFor' result for the 'example-2' datasource") must_== false  // never set
    database.hasValuesFor("example-3").aka("the 'hasValuesFor' result for the 'example-3' datasource") must_== true
    database.hasValuesFor("example-4").aka("the 'hasValuesFor' result for the 'example-4' datasource") must_== false  // never set
  }

  def createDefinition(file: File, datasources: String*): RrdDef = {
    val definition = new RrdDef(file, Seconds(0), frequency)
    datasources.foreach { name =>
      definition.addDatasource(new DsDef(name, GAUGE, frequency, NaN, NaN))
    }
    definition.addArchive(Archive(aDay, frequency, frequency))
    definition
  }

  private def createRrdFile(definition: RrdDef) {
    val database = new RrdDb(definition)
    database.close()
    println(definition.dump())
  }

  private def update(file: File, time: Seconds, values: Double*) = {
    val database = new RrdDb(file)
    database.createSample().setValues(database, time, values:_*)
    database.close()
  }

}
