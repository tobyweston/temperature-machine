package bad.robot.temperature

import bad.robot.temperature.rrd._

import scala.concurrent.duration.Duration

case class XmlExport(period: Duration)(implicit numberOfSensors: Int) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    Xml.export(currentTime - period.toSeconds, currentTime, numberOfSensors)
  }
}
