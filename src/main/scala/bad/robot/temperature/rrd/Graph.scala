package bad.robot.temperature.rrd

import java.awt.Color
import java.awt.Color._

import bad.robot.temperature.rrd.RrdUpdate._
import org.rrd4j.ConsolFun._
import org.rrd4j.graph.{RrdGraph, RrdGraphDef}

object Graph {

  private val colours = Array(
    new Color(69,  113, 167),   // blue
    new Color(170, 70,  67),    // red
    new Color(137, 165, 78),    // green
    new Color(128, 105, 155),   // purple
    new Color(61,  150, 174),   // cyan
    new Color(219, 132, 61)     // orange
  )

  val path = RrdFile.path

  def create(from: Seconds, to: Seconds, hosts: List[Host], numberOfSensors: Int) = {
    val graph = new RrdGraphDef()
    graph.setWidth(800)
    graph.setHeight(500)
    graph.setFilename(path / s"temperature-${(to - from).toDays}-days.png")
    graph.setStartTime(from)
    graph.setEndTime(to)
    graph.setTitle("Temperature")
    graph.setVerticalLabel("°C")

    for (host <- hosts; sensor <- 1 to numberOfSensors) {
      val name = s"${host.name}-sensor-$sensor"
      graph.datasource(name, RrdFile.file, name, AVERAGE)

      graph.line(name, colours(sensor - 1))

      graph.gprint(name, MIN, "min = %.2f%s °C")
      graph.gprint(name, MAX, "max = %.2f%s °C")
    }

    graph.setImageFormat("png")

    graph.hrule(0, new Color(204, 255, 255), "Freezing")
    graph.hspan(16, 20, transparent(green), "Optimal")

    val file = new RrdGraph(graph)
//    println(file.getRrdGraphInfo.getFilename)
//    println(file.getRrdGraphInfo.dump())
  }

  def transparent(color: Color): Color = {
    val rgb = color.getRGB
    val red = (rgb >> 16) & 0xFF
    val green = (rgb >> 8) & 0xFF
    val blue = rgb & 0xFF
    new Color(red, green, blue, 0x33)
  }
}