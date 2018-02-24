package bad.robot.temperature.task

import bad.robot.temperature.rrd.Seconds.now
import bad.robot.temperature.rrd._

import scala.concurrent.duration.Duration
import bad.robot.logging._
import scala.concurrent.duration._
import scala.language.postfixOps

case class GenerateGraph(period: Duration)(implicit hosts: List[Host]) extends Runnable {
  def run(): Unit = {
    val currentTime = now()
    if (period > (24 hours)) Log.debug(s"Generating RRD chart for last $period")
    Graph.create(currentTime - period.toSeconds, currentTime, hosts, s"Last $period")
  }
}


