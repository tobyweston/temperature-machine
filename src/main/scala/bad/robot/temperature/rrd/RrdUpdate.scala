package bad.robot.temperature.rrd

import java.io.{PrintWriter, StringWriter}

import bad.robot.temperature.{Error, Measurement, RrdError, SensorTemperature, Temperature}
import org.rrd4j.core.RrdDb

import scala.{Error => _}
import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(monitored: List[Host], measurement: Measurement) {

  private val UnknownValues = List().padTo(monitored.size * RrdFile.MaxSensors, SensorTemperature("Unknown", Temperature(Double.NaN)))

  def apply(): Error \/ Unit = {
    fromTryCatchNonFatal {
      val database = new RrdDb(RrdFile.file)
      val sample = database.createSample()
      val temperatures = UnknownValues.patch(monitored.indexOf(measurement.host) * RrdFile.MaxSensors, measurement.temperatures, measurement.temperatures.size)
      sample.setValues(database, measurement.time, temperatures.map(_.temperature.celsius): _*)
      database.close()
    }.leftMap(error => {
      RrdError(messageOrStackTrace(error))
    })
  }

  def messageOrStackTrace(error: Throwable): String = {
    Option(error.getMessage).getOrElse {
      val writer = new StringWriter()
      error.printStackTrace(new PrintWriter(writer))
      writer.toString
    }
  }
}
