package bad.robot.temperature.rrd

import java.awt.Color._

import bad.robot.temperature.rrd.RrdUpdate._
import org.rrd4j.ConsolFun._
import org.rrd4j.graph.{RrdGraph, RrdGraphDef}

import scala.concurrent.duration.Duration


object Graph {

  val path = "temperature.png"

  def create(from: Seconds, to: Seconds) = {
    val graph = new RrdGraphDef()
    graph.setWidth(800)
    graph.setHeight(500)
    graph.setFilename(path)
    graph.setStartTime(from)
    graph.setEndTime(to)
    graph.setTitle("Temperature")
    graph.setVerticalLabel("°C")

    graph.datasource(name, RrdFile.path, name, AVERAGE)
    graph.line(name, blue)
    graph.setImageFormat("png")

    graph.gprint(name, MIN, "min = %.2f%s °C")
    graph.gprint(name, MAX, "max = %.2f%s °C")

    graph.hrule(0, green.darker(), "freezing")

    val file = new RrdGraph(graph)
    println(file.getRrdGraphInfo.getFilename)
    println(file.getRrdGraphInfo.dump())
  }
}

object GenerateGraph extends App {

  val period = Duration(5, "minutes")

  val start = now() - period.toSeconds
//  Graph.create(start, start + period.toSeconds)

  val s = Seconds(1450209809)
  Graph.create(s, s + Duration(8, "hours").toSeconds)

}