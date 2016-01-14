package bad.robot.temperature.rrd

import bad.robot.temperature.Temperature
import bad.robot.temperature.rrd.Seconds.secondsToLong

import scala.concurrent.duration.Duration
import scala.util.Random

object Example extends App {

  val random = new Random()

  val duration = Duration(1, "days")

  val start = now() - duration.toSeconds
  val end = now()

  val frequency = Duration(30, "seconds")

  RrdFile(frequency).create(start - 5)

  val seed = random.nextInt(30) + random.nextDouble()

  def smooth = (value: Double) => if (random.nextDouble() > 0.5) value + random.nextDouble() else value - random.nextDouble()

  val temperatures = Stream.iterate(seed)(smooth)
  val times = Stream.iterate(start)(_ + frequency.toSeconds).takeWhile(_ < end)
  times.zip(temperatures).foreach({
    case (time, celsius) => {
      RrdUpdate(time, List(Temperature(celsius), Temperature(celsius + 4.3))).apply()
    }
  })

  val numberOfSensors = 2

  Xml.export(start, start + aDay, numberOfSensors)

  Graph.create(start, start + aDay, numberOfSensors)
  Graph.create(start, start + aDay * 2, numberOfSensors)
  Graph.create(start, start + aWeek, numberOfSensors)
  Graph.create(start, start + aMonth, numberOfSensors)

  println("Done generating " + duration)
}