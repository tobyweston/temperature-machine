package bad.robot.temperature.task

import bad.robot.temperature.rrd._
import bad.robot.temperature.rrd.Seconds.now

import scala.concurrent.duration.Duration
import bad.robot.logging._

case class GenerateGraph(period: Duration)(implicit hosts: List[Host]) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    Log.debug(s"Generating RRD chart for last $period")
    Graph.create(currentTime - period.toSeconds, currentTime, hosts)
  }
}


