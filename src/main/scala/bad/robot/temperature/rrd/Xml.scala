package bad.robot.temperature.rrd

import java.io.{BufferedWriter, FileWriter}

import bad.robot.temperature.encode
import bad.robot.temperature.rrd.ChartJson._
import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb

import scala.collection.JavaConverters._
import scala.xml.{Elem, XML}


object Xml {
  def export(start: Seconds, end: Seconds, hosts: List[Host], filename: Option[String] = Some("temperature.xml")): Elem = {
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
    saveFileIfRequired(filename, xml)
    XML.loadString(xml)
  }

  def toJson(xml: Elem): String = {
    val series = parse(xml)
    encode(series).spaces2
  }

  private def saveFileIfRequired(filename: Option[String], xml: String) = {
    filename.foreach(path => {
      val file = RrdFile.path / path
      val writer = new BufferedWriter(new FileWriter(file))
      writer.write(xml)
      writer.close()
    })
  }

  def nonEmpty(sensors: List[String], database: RrdDb) = sensors.filter(database.hasValuesFor).toSet

}