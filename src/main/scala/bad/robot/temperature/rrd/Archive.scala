package bad.robot.temperature.rrd

import org.rrd4j.ConsolFun._
import org.rrd4j.core.ArcDef

import scala.concurrent.duration.Duration

object Archive {

  def apply(duration: Duration, frequency: Seconds, aggregating: Duration): Archive = {

    def numberOfSteps = (aggregating.toSeconds / frequency.value).toInt

    def numberOfRows = (duration.toSeconds / (numberOfSteps * frequency.value)).toInt

    Archive(numberOfSteps, numberOfRows)
  }

  implicit def archiveToArcDef(archive: Archive): ArcDef = new ArcDef(AVERAGE, 0.5, archive.steps, archive.rows)

}

case class Archive(steps: Int, rows: Int)