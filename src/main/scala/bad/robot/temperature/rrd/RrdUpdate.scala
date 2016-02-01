package bad.robot.temperature.rrd

import bad.robot.temperature.Error
import bad.robot.temperature.{RrdError, Temperature}
import scala.{Error => _}
import org.rrd4j.core.RrdDb
import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(time: Seconds, temperatures: List[Temperature]) {
  def apply(): Error \/ Unit = {
    fromTryCatchNonFatal {
      val database = new RrdDb(RrdFile.file)
      val sample = database.createSample()
      sample.setTime(time)
      sample.setValues(temperatures.map(_.celsius): _*)
      sample.update()
      database.close()
    }.leftMap(error => RrdError(error.getMessage))
  }
}
