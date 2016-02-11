package bad.robot.temperature.rrd

import bad.robot.temperature.{Temperature, Error, Measurement, RrdError}
import org.rrd4j.core.RrdDb

import scala.{Error => _}
import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(measurement: Measurement) {

  private val UnknownValues = List().padTo(allHosts.size * RrdFile.MaxSensors, Temperature(Double.NaN))

  val allHosts = List(Host("bedroom"), Host("lounge")) // TODO pass in from command line args; the expected list of clients

  def apply(): Error \/ Unit = {
    fromTryCatchNonFatal {
      val database = new RrdDb(RrdFile.file)
      val sample = database.createSample()
      val temperatures = UnknownValues.patch(allHosts.indexOf(measurement.host) * RrdFile.MaxSensors, measurement.temperatures, measurement.temperatures.size)
      sample.setTime(measurement.time)
      sample.setValues(temperatures.map(_.celsius): _*)
      sample.update()
      database.close()
    }.leftMap(error => RrdError(error.getMessage))

  }
}
