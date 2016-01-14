package bad.robot.temperature

import bad.robot.temperature.rrd._

import scala.concurrent.duration.Duration

case class GenerateGraph(period: Duration)(implicit numberOfSensors: Int) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    Graph.create(currentTime - period.toSeconds, currentTime, numberOfSensors)
  }
}


