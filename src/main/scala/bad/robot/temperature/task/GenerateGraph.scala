package bad.robot.temperature.task

import bad.robot.temperature.rrd._
import bad.robot.temperature.rrd.Seconds.now

import scala.concurrent.duration.Duration

case class GenerateGraph(period: Duration)(implicit hosts: List[Host]) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    Graph.create(currentTime - period.toSeconds, currentTime, hosts)
  }
}


