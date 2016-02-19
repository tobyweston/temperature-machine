package bad.robot.temperature.rrd

import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb
import scala.collection.JavaConverters._


object Xml {
  def export(start: Seconds, end: Seconds, hosts: List[Host]) = {
    val path = RrdFile.path / "temperature.xml"
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
    data.exportXml(path)
  }

  def nonEmpty(sensors: List[String], database: RrdDb) = sensors.filter(database.hasValuesFor).toSet

}