package bad.robot.temperature.rrd

import java.awt.Color
import java.awt.Color._

import bad.robot.temperature.rrd.RpnGenerator._
import bad.robot.temperature.rrd.RrdFile.MaxSensors
import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb
import org.rrd4j.graph.{RrdGraph, RrdGraphDef}

object Graph {

  class CircularArray[T](array: Array[T]) {
    private var index = -1
    def next: T = {
      if (index == array.length - 1)
        index = 0
      else
        index = index + 1
      array(index)
    }
  }

  private val colours = new CircularArray(Array(
    new Color(69,  114, 167),   // #4572A7 san marino (blue)
    new Color(170, 70,  67),    // #AA4643 roof terracotta (red)
    new Color(137, 165, 78),    // #89A54E chelsea cucumber (green)
    new Color(128, 105, 155),   // #80699B deluge (purple)
    new Color(61,  150, 174),   // #3D96AE boston blue
    new Color(219, 132, 61),    // #DB843D tree poppy (orange)
    new Color(146, 168, 205),   // #92A8CD polo blue
    new Color(164, 125, 124),   // #A47D7C opium
    new Color(181, 202, 146)    // #B5CA92 sprout
  ))

  val path = RrdFile.path

  def create(from: Seconds, to: Seconds, hosts: List[Host]) = {
    val graph = new RrdGraphDef()
    graph.setWidth(800)
    graph.setHeight(500)
    graph.setFilename(path / s"temperature-${(to - from).toDays}-days.png")
    graph.setStartTime(from)
    graph.setEndTime(to)
    graph.setTitle("Temperature")
    graph.setVerticalLabel("°C")

    val all = for {
      host   <- hosts
      sensor <- 1 to MaxSensors
    } yield s"${host.name}-sensor-$sensor"

    val sensors = all.filter(new RrdDb(RrdFile.file).hasValuesFor)
    sensors.foreach(name => {
      graph.datasource(name, RrdFile.file, name, AVERAGE)
      graph.line(name, colours.next, name)
    })

    graph.comment("\\l")
    graph.hrule(0, new Color(51, 153, 255), "Freezing")
    graph.hspan(16, 20, transparent(green), "Optimal\\j")

    hosts.map(host => host -> sensors.filter(_.contains(host.name))).foreach({
      case (_, Nil)               => ()
      case (host, (sensor :: Nil))     =>
        graph.gprint(sensor, MIN, s"${host.name} min = %.2f%s °C")
        graph.gprint(sensor, MAX, s"${host.name} max = %.2f%s °C\\j")
      case (host, sensorsForHost) =>
        graph.datasource(s"${host.name}-max", generateRpn(sensorsForHost, Max))
        graph.datasource(s"${host.name}-min", generateRpn(sensorsForHost, Min))
        graph.gprint(s"${host.name}-min", MIN, s"${host.name} min = %.2f%s °C")
        graph.gprint(s"${host.name}-max", MAX, s"${host.name} max = %.2f%s °C\\j")
    })

    graph.setImageFormat("png")

    new RrdGraph(graph)
  }

  def transparent(color: Color): Color = {
    val rgb = color.getRGB
    val red = (rgb >> 16) & 0xFF
    val green = (rgb >> 8) & 0xFF
    val blue = rgb & 0xFF
    new Color(red, green, blue, 0x33)
  }
}