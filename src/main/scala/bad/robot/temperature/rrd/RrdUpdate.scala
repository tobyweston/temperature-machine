package bad.robot.temperature.rrd

import bad.robot.logging.Log
import bad.robot.temperature.{Error, Measurement, RrdError, SensorReading, Temperature, _}
import org.rrd4j.core.RrdDb

import scalaz.{\/, \/-}
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(monitored: List[Host]) {

  private val UnknownValues = List().padTo(monitored.size * RrdFile.MaxSensors, SensorReading("Unknown", Temperature(Double.NaN)))

  def apply(measurement: Measurement): Error \/ Unit = {
    indexOf(measurement) match {
      case Some(index) => writeTemperatureAt(measurement, index)
      case None        => Log.error(s"Unable to write temperature to RRD: '${measurement.host.name}' not setup as an archive"); \/-(())
    }
  }

  private def writeTemperatureAt(measurement: Measurement, index: Int) = {
    fromTryCatchNonFatal {
      val database = new RrdDb(RrdFile.file)
      val sample = database.createSample()
      val temperatures = UnknownValues.patch(index * RrdFile.MaxSensors, measurement.temperatures, measurement.temperatures.size)
      sample.setValues(database, measurement.time, temperatures.map(_.temperature.celsius): _*)
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
