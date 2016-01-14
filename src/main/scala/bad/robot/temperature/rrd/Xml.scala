package bad.robot.temperature.rrd

import org.rrd4j.ConsolFun._
import org.rrd4j.core.RrdDb
import scala.collection.JavaConverters._


object Xml {
  def export(start: Seconds, end: Seconds, numberOfSensors: Int) = {
    val path = RrdFile.path / "temperature.xml"
    val database = new RrdDb(RrdFile.file)
    val request = database.createFetchRequest(AVERAGE, start, end)
    val sensors = Stream.iterate(1)(_ + 1).take(numberOfSensors).map(sensor => s"sensor-$sensor").toSet.asJava
    request.setFilter(sensors)
    val data = request.fetchData()
    data.exportXml(path)
  }
}