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

  def smooth = (value: Double) => if (random.nextDouble() < 0.55) value + random.nextDouble() else value - random.nextDouble()

  val temperatures = Stream.iterate(seed)(smooth)
  val times = Stream.iterate(start)(_ + frequency.toSeconds).takeWhile(_ < end)
  times.zip(temperatures).foreach({
    case (time, celsius) => RrdTemperature(time, Temperature(celsius)).apply()
  })

//  fetchData()
  generateGraph()

  def generateGraph(): Unit = {
    Graph.create(start, start + aDay.toSeconds)
  }

  def fetchData() = {
    val database = new RrdDb(RrdFile.path)
    println("last update " + new Date(database.getLastUpdateTime))
    println(database.getInfo)
    val request = database.createFetchRequest(AVERAGE, start, end)
    val data = request.fetchData()
    println("rows : " + data.getRowCount)
    println(data.exportXml())
  }
}
