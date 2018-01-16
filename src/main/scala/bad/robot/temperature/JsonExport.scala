package bad.robot.temperature

import bad.robot.temperature.rrd._
import bad.robot.temperature.rrd.Seconds.now
import bad.robot.temperature.server.JsonFile

import scala.concurrent.duration.Duration

case class JsonExport(period: Duration)(implicit hosts: List[Host]) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    val xml = Xml(currentTime - period.toSeconds, currentTime, hosts)
    xml.exportJson(JsonFile.filename)
  }
}
