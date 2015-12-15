package bad.robot.temperature.rrd

import java.util.Date

import bad.robot.temperature.Temperature
import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb

import scala.util.Random

object RrdFetch extends App {

  val random = new Random()

  val start = 1450166400L
  val end = now()

  RrdFile().create(start - 5)

  Stream.iterate(start)(_ + 5).takeWhile(_ < end).foreach(time => {
    RrdTemperature(time, Temperature(celsius = random.nextInt(30) + random.nextDouble())).apply()
  })

  val database = new RrdDb(RrdFile.path)
  println("last update " + new Date(database.getLastUpdateTime))
  println(database.getInfo)
  val request = database.createFetchRequest(AVERAGE, start, end)
  val data = request.fetchData()
  println("rows : " + data.getRowCount)
  println(data.exportXml())

  generateGraph()

  def generateGraph(): Unit = {
    Graph.create(start, end)
  }
}
