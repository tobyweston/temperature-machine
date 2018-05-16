package bad.robot.temperature.rrd

import java.io.{BufferedWriter, FileWriter}

import bad.robot.temperature.{FileOps, Files, JsonOps, encode}
import bad.robot.temperature.rrd.ChartJson._
import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb
import bad.robot.temperature.Files._
import scala.collection.JavaConverters._
import scala.xml.{Elem, XML}

case class Xml(xml: Elem) {
  def exportXml(filename: String) = {
    XML.save(Files.path / filename, xml)
  }

  def exportJson(filename: String) = {
    val writer = new BufferedWriter(new FileWriter(Files.path / filename))
    writer.write(toJson())
    writer.close()
  }

  def toJson(): String = {
    val series = parse(xml)
    encode(series).spaces2ps
  }

}

object Xml {
  def apply(start: Seconds, end: Seconds, hosts: List[Host]): Xml = {
    val database = new RrdDb(RrdFile.file)
    val request = database.createFetchRequest(AVERAGE, start, end)
    val sensors = for {
      host   <- hosts
      sensor <- 1 to RrdFile.MaxSensors
    } yield {
      s"${host.name}-sensor-$sensor"
    }
    request.setFilter(nonEmpty(sensors, database).asJava)
    val data = request.fetchData()
    val xml = data.exportXml()
    new Xml(XML.loadString(xml))
  }

  def nonEmpty(sensors: List[String], database: RrdDb) = sensors.filter(database.hasValuesFor).toSet

}