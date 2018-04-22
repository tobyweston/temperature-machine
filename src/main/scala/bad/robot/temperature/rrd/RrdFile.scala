package bad.robot.temperature.rrd

import java.io.File

import bad.robot.logging._
import bad.robot.temperature.FileOps
import bad.robot.temperature.rrd.RrdFile._
import bad.robot.temperature.rrd.Seconds.now
import cats.effect.IO
import org.rrd4j.DsType.GAUGE
import org.rrd4j.core._

import scala.Double._
import scala.concurrent.duration.Duration


object RrdFile {
  val path = new File(sys.props("user.home")) / ".temperature"
  val file = path / "temperature.rrd"

  val MaxSensors = 5

  path.mkdirs()

  def exists = IO(file.exists())

}

case class RrdFile(hosts: List[Host], frequency: Seconds = Duration(30, "seconds"), file: File = RrdFile.file) {

  def create(start: Seconds = now() - Seconds(1)) = {

    val heartbeat = frequency + Seconds(5)

    val definition = new RrdDef(file, start, frequency)

    val daily             = Archive(aDay, frequency, frequency)     // = new ArcDef(AVERAGE, 0.5, 1, 2880)
    val weeklyHourAvg     = Archive(aWeek, frequency, anHour)       // = new ArcDef(AVERAGE, 0.5, 120, 168)
    val monthlyTwoHourAvg = Archive(aMonth, frequency, anHour * 2)  // = new ArcDef(AVERAGE, 0.5, 240, 360)

    for (host <- hosts; sensor <- 1 to MaxSensors) {
      definition.addDatasource(new DsDef(s"${host.name}-sensor-$sensor", GAUGE, heartbeat, NaN, NaN))
    }

    definition.addArchive(daily)
    definition.addArchive(weeklyHourAvg)
    definition.addArchive(monthlyTwoHourAvg)

    createFile(definition)
  }

  private def createFile(definition: RrdDef) = {
    val database = new RrdDb(definition)
    database.close()
    Log.info(definition.dump())
  }

}