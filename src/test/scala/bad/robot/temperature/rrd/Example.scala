package bad.robot.temperature.rrd

import bad.robot.temperature.{SensorId, Temperature}

import scala.concurrent.duration.Duration
import scala.util.Random
import Seconds.secondsToLong

object Example extends App {

  val random = new Random()

  val duration = Duration(1, "days")

  val start = now() - duration.toSeconds
  val end = now()

  val frequency = Duration(30, "seconds")

  RrdFile(frequency).create(start - 5)

  val seed = random.nextInt(30) + random.nextDouble()

  def smooth = (value: Double) => if (random.nextDouble() > 0.5) value + random.nextDouble() else value - random.nextDouble()

  val sensors = List(
    SensorId("front room"),
    SensorId("living room")
  )

  val temperatures = Stream.iterate(seed)(smooth)
  val times = Stream.iterate(start)(_ + frequency.toSeconds).takeWhile(_ < end)
  times.zip(temperatures).foreach({
    case (time, celsius) => {
      RrdUpdate(sensors(0), time, Temperature(celsius)).apply()
      RrdUpdate(sensors(1), time + Seconds(1), Temperature(celsius + 2.5)).apply()
    }
  })

  val numberOfSensors = sensors.length

  Xml.export(start, start + aDay, numberOfSensors)

  Graph.create(start, start + aDay, numberOfSensors)
  Graph.create(start, start + aDay * 2, numberOfSensors)
  Graph.create(start, start + aWeek, numberOfSensors)
  Graph.create(start, start + aMonth, numberOfSensors)

  println("Done generating " + duration)
}