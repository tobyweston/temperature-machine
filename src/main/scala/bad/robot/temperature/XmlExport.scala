package bad.robot.temperature

import bad.robot.temperature.rrd._
import bad.robot.temperature.rrd.Seconds.now

import scala.concurrent.duration.Duration

case class XmlExport(period: Duration)(implicit hosts: List[Host]) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    val xml = Xml(currentTime - period.toSeconds, currentTime, hosts)
    xml.exportXml("temperature.xml")
  }
}
