package bad.robot.temperature.rrd

import java.awt.Color

import bad.robot.temperature.rrd.RrdTemperature._
import org.rrd4j.ConsolFun._
import org.rrd4j.graph.{RrdGraph, RrdGraphDef}


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
    graph.line(name, Color.blue)
    graph.setImageFormat("png")

    graph.gprint(name, MIN, "min = %.2f%s °C")
    graph.gprint(name, MAX, "max = %.2f%s °C")

    val file = new RrdGraph(graph)
    println(file.getRrdGraphInfo.getFilename)
    println(file.getRrdGraphInfo.dump())
  }
}
