package bad.robot.temperature.rrd

import java.util.Date

import bad.robot.temperature.Temperature
import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb

import scala.concurrent.duration.Duration
import scala.util.Random

object Example extends App {

  val random = new Random()

  val start = 1450166400L
  val end = start + Duration(1, "days").toSeconds

  val frequency = Duration(30, "seconds")

  RrdFile(frequency).create(start - 5)

  val seed = random.nextInt(30) + random.nextDouble()

  def smooth = (value: Double) => if (random.nextDouble() > 0.5) value + random.nextDouble() else value - random.nextDouble()

  val temperatures = Stream.iterate(seed)(smooth)
  val times = Stream.iterate(start)(_ + frequency.toSeconds).takeWhile(_ < end)
  times.zip(temperatures).foreach({
    case (time, celsius) => RrdUpdate(time, Temperature(celsius)).apply()
  })

  Xml.export(start, start + aDay.toSeconds)
  Graph.create(start, start + aDay.toSeconds)

}