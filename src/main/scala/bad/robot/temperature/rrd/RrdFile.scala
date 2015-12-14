package bad.robot.temperature.rrd

import java.awt.Color
import java.util.Date

import bad.robot.temperature.{Temperature, TemperatureWriter}
import org.rrd4j.ConsolFun._
import org.rrd4j.DsType.GAUGE
import org.rrd4j.core._
import org.rrd4j.graph.{RrdGraph, RrdGraphDef}

import scala.Double._
import scala.concurrent.duration.Duration

object RrdFile {
  val path = "/Users/toby/Workspace/bitbucket/temperature-machine/temperature.rrd"
}

case class RrdFile (step: Duration = Duration(30, "seconds")) {

  def create() = {
    val heartbeat = step + Duration(5, "seconds")
    val aDay = Duration(24, "hours")
    val file = new RrdDef(RrdFile.path, step.toSeconds)
    file.addDatasource(new DsDef("temperature-1", GAUGE, heartbeat.toSeconds, NaN, NaN))
    file.addArchive(new ArcDef(AVERAGE, 0.5, 1, (aDay / step).toInt))
    createFile()

    def createFile() = {
      val database = new RrdDb(file)
      database.close()
      println("File created; " + file.dump())
    }
  }
  
}

object Rrd {
  val temperatureProbe = "temperature-1"
}

case class Rrd() extends TemperatureWriter {

  def write(temperature: Temperature): Unit = {
    val database = new RrdDb(RrdFile.path)
    val sample = database.createSample()
    sample.setTime(new Date().getTime)
    sample.setValue(Rrd.temperatureProbe, temperature.celsius)
    print(temperature)
    sample.update()
    database.close()
  }

  def close(): Unit = {
//    database.close()
  }
}

object Graph {

  val path = "/Users/toby/Workspace/bitbucket/temperature-machine/temperature.png"

  def now = new Date().getTime

  def create() = {
    val graph = new RrdGraphDef()
    graph.setWidth(500)
    graph.setHeight(300)
    graph.setFilename(path)
    graph.setStartTime(now - Duration(5, "minutes").toMillis)
    graph.setEndTime(now)
    graph.setTitle("Temperature")
    graph.setVerticalLabel("°C")

    graph.datasource("temp", RrdFile.path, Rrd.temperatureProbe, AVERAGE)
    graph.hrule(2568, Color.GREEN, "hrule")
    graph.setImageFormat("png")

    new RrdGraph(graph)
  }
}

object GenerateGraph extends App {
  Graph.create()
}