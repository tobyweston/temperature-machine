package bad.robot.temperature.rrd

import java.util.Date

import bad.robot.temperature.rrd.RrdTemperature._
import bad.robot.temperature.{Temperature, TemperatureWriter}
import org.rrd4j.ConsolFun._
import org.rrd4j.DsType.GAUGE
import org.rrd4j.core._

import scala.Double._
import scala.concurrent.duration.Duration


object RrdFile {
  val path = "temperature.rrd"
}

case class RrdFile(step: Duration = Duration(30, "seconds")) {

  def create(start: Seconds = now()) = {
    val heartbeat = step + Duration(5, "seconds")
    val aDay = Duration(24, "hours")
    val rows = (aDay / step.toSeconds).toSeconds.toInt

    println("rows " + rows)

    val definition = new RrdDef(RrdFile.path, start, step.toSeconds)
    definition.addDatasource(new DsDef(name, GAUGE, heartbeat.toSeconds, NaN, NaN))
    definition.addArchive(new ArcDef(AVERAGE, 0.5, 1, rows))
    createFile()

    def createFile() = {
      val database = new RrdDb(definition)
      database.close()
      println("File created; " + definition.dump())
    }
  }
}