package bad.robot.temperature.rrd

§import java.io.File

import bad.robot.logging.Log
import bad.robot.temperature.task.FixedTimeMeasurement
import bad.robot.temperature.{Error, Measurement, RrdError, SensorReading, Temperature, _}
import org.rrd4j.core.RrdDb

import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(monitored: List[Host], rrd: File = RrdFile.file) {

  private val NoValues = List().padTo(monitored.size * RrdFile.MaxSensors, SensorReading("Unknown", Temperature(Double.NaN)))

  def apply(measurements: FixedTimeMeasurement): Error \/ List[SensorReading] = {
    val readings = patched(measurements)
    writeTemperatureAt(measurements.time, readings).map(_ => readings)
  }

  private def patched(measurement: FixedTimeMeasurement): List[SensorReading] = {
    measurement.measurements.foldLeft(NoValues)((accumulator, measurement) => {
      indexOf(measurement) match {
        case Some(index) => accumulator.patch(index * RrdFile.MaxSensors, measurement.temperatures, measurement.temperatures.size)
        case None        => Log.error(s"Unable to write temperature for '${measurement.host.name}' to RRD: host not setup as an archive"); accumulator
      }
    })
  }

  private def writeTemperatureAt(time: Seconds, temperatures: List[SensorReading]): Error \/ Unit = {
    fromTryCatchNonFatal {
      val database = new RrdDb(rrd)
      val sample = database.createSample()
      sample.setValues(database, time, temperatures.map(_.temperature.celsius): _*)
      database.close()
    }.leftMap(error => {
      RrdError(messageOrStackTrace(error))
    })
  }

  private def indexOf(measurement: Measurement): Option[Int] = {
    val index = monitored.map(_.name).indexOf(measurement.host.name)
    if (index == -1) None else Some(index)
  }

  private def messageOrStackTrace(error: Throwable): String = {
    Option(error.getMessage).getOrElse(stackTraceOf(error))
  }
}
