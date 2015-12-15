package bad.robot.temperature.rrd

import java.util.Date

import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb

import scala.concurrent.duration.Duration

object Xml {
  def export(start: Seconds, end: Seconds) = {
    val database = new RrdDb(RrdFile.path)
    println("last update " + new Date(database.getLastUpdateTime))
    println(database.getInfo)
    val request = database.createFetchRequest(AVERAGE, start, end)
    val data = request.fetchData()
    println(data.exportXml())
    println("rows : " + data.getRowCount)
  }
}

object ExportXml extends App {

  val period = Duration(5, "minutes")

  val start = now() - period.toSeconds
  Xml.export(start, start + period.toSeconds)

}