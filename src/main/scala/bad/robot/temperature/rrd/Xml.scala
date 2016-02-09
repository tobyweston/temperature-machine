package bad.robot.temperature.rrd

import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb
import scala.collection.JavaConverters._


object Xml {
  def export(start: Seconds, end: Seconds, hosts: List[Host], numberOfSensors: Int) = {
    val path = RrdFile.path / "temperature.xml"
    val database = new RrdDb(RrdFile.file)
    val request = database.createFetchRequest(AVERAGE, start, end)
    val sensors = for {
      host   <- hosts
      sensor <- 1 to numberOfSensors
    } yield {
      s"${host.name}-sensor-$sensor"
    }
    request.setFilter(sensors.toSet.asJava)
    val data = request.fetchData()
    data.exportXml(path)
  }
}