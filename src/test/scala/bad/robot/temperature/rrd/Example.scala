package bad.robot.temperature.rrd

import bad.robot.temperature.{Measurement, Temperature}
import bad.robot.temperature.rrd.Seconds.secondsToLong
import bad.robot.temperature.Error
import scala.concurrent.duration.Duration
import scala.util.Random
import scala.{Error => _}
import scalaz.{\/, -\/}

object Example extends App {

  val random = new Random()

  val duration = Duration(1, "days")

  val start = now() - duration.toSeconds
  val end = now()

  val frequency = Duration(30, "seconds")

  val hosts = List(Host("bedroom"), Host("lounge"))

  RrdFile(hosts, frequency).create(start - 5)

  populateRrd(hosts)

  val numberOfSensors = 2

  Xml.export(start, start + aDay, hosts, numberOfSensors)

  Graph.create(start, start + aDay, hosts, numberOfSensors)
  Graph.create(start, start + aDay * 2, hosts, numberOfSensors)
  Graph.create(start, start + aWeek, hosts, numberOfSensors)
  Graph.create(start, start + aMonth, hosts, numberOfSensors)

  println("Done generating " + duration)


  def populateRrd(hosts: List[Host]) = {
    def seed = random.nextInt(30) + random.nextDouble()
    def smooth = (value: Double) => if (random.nextDouble() > 0.5) value + random.nextDouble() else value - random.nextDouble()

    val temperatures = Stream.iterate(seed)(smooth).zip(Stream.iterate(seed)(smooth))
    val times = Stream.iterate(start)(_ + frequency.toSeconds).takeWhile(_ < end)

    times.zip(temperatures).foreach({
      case (time, (temperature1, temperature2)) => {
        handleError(RrdUpdate(hosts, Measurement(hosts(0), time,     List(Temperature(temperature1), Temperature(temperature1 + 6.3)))).apply())
        handleError(RrdUpdate(hosts, Measurement(hosts(1), time + 1, List(Temperature(temperature2), Temperature(temperature2 + 1.3)))).apply())
      }
    })

    def handleError(f: => Error \/ Unit): Unit = {
      f match {
        case -\/(error) => println(error)
        case _ => ()
      }
    }
  }

}