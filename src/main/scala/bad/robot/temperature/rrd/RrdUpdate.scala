package bad.robot.temperature.rrd

import bad.robot.temperature.{Temperature, Error, Measurement, RrdError}
import org.rrd4j.core.RrdDb

import scala.{Error => _}
import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(monitored: List[Host], measurement: Measurement) {

  private val UnknownValues = List().padTo(monitored.size * RrdFile.MaxSensors, Temperature(Double.NaN))

  def apply(): Error \/ Unit = {
    fromTryCatchNonFatal {
      val database = new RrdDb(RrdFile.file)
      val sample = database.createSample()
      val temperatures = UnknownValues.patch(monitored.indexOf(measurement.host) * RrdFile.MaxSensors, measurement.temperatures, measurement.temperatures.size)
      sample.setTime(measurement.time)
      sample.setValues(temperatures.map(_.celsius): _*)
      sample.update()
      database.close()
    }.leftMap(error => RrdError(error.getMessage))

  }
}
