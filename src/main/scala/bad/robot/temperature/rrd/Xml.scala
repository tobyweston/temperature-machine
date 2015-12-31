package bad.robot.temperature.rrd

import java.util.Date

import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb

import scala.concurrent.duration.Duration

object Xml {
  def export(start: Seconds, end: Seconds) = {
    val path = RrdFile.path / "temperature.xml"
    val database = new RrdDb(RrdFile.file)
    println("last update " + new Date(database.getLastUpdateTime))
    val request = database.createFetchRequest(AVERAGE, start, end)
    val data = request.fetchData()
    val xml = data.exportXml(path)
//    println(s"XML data saved, #rows = ${data.getRowCount}")
    xml
  }
}

object ExportXml extends App {

  val period = Duration(15, "minutes")

  val start = now() - period.toSeconds
//  Xml.export(start, start + period.toSeconds)

  val s = Seconds(1451420961)
  Xml.export(s, s + Duration(12, "hours").toSeconds)
}