package bad.robot.temperature.rrd

import bad.robot.temperature.rrd.Seconds.{now, secondsToLong}
import bad.robot.temperature.server.JsonFile
import bad.robot.temperature.{Error, Measurement, SensorReading, Temperature}

import scala.concurrent.duration.Duration
import scala.util.Random
import scalaz.{-\/, \/}

object Example extends App {

  sys.props += ("org.slf4j.simpleLogger.defaultLogLevel" -> "info")

  val random = new Random()

  val duration = Duration(1, "days")

  val start = now() - duration.toSeconds
  val end = now()

  val frequency = Duration(30, "seconds")

  val hosts = List(Host("bedroom", None), Host("lounge", None))

  RrdFile(hosts, frequency).create(start - 5)

  populateRrd(hosts)

  val xml = Xml(start, start + aDay, hosts)
  xml.exportJson(JsonFile.filename)
  xml.exportXml("temperature.xml")

  Graph.create(start, start + aDay, hosts, "A day")
  Graph.create(start, start + aDay * 2, hosts, "2 days")
  Graph.create(start, start + aWeek, hosts, "A week")
  Graph.create(start, start + aMonth, hosts, "A month")

  println("Done generating " + duration)


  def populateRrd(hosts: List[Host]) = {
    def seed = random.nextInt(30) + random.nextDouble()
    def smooth = (value: Double) => if (random.nextDouble() > 0.5) value + random.nextDouble() else value - random.nextDouble()

    val temperatures = Stream.iterate(seed)(smooth).zip(Stream.iterate(seed)(smooth))
    val times = Stream.iterate(start)(_ + frequency.toSeconds).takeWhile(_ < end)

    times.zip(temperatures).foreach({
      case (time, (temperature1, temperature2)) => {
        handleError(RrdUpdate(hosts).apply(Measurement(hosts(0), time, List(
          SensorReading("?", Temperature(temperature1)),
          SensorReading("?", Temperature(temperature1 + 6.3)))
        )))
        handleError(RrdUpdate(hosts).apply(Measurement(hosts(1), time + 1, List(
          SensorReading("?", Temperature(temperature2)),
          SensorReading("?", Temperature(temperature2 + 1.3)))
        )))
      }
    })

    def handleError(f: => Error \/ Unit): Unit = {
      f match {
        case -\/(error) => println(error)
        case _          => ()
      }
    }
  }

}