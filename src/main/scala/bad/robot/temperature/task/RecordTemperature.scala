package bad.robot.temperature.task

import bad.robot.temperature.server.AllTemperatures
import bad.robot.temperature.{Error, FixedTimeMeasurementWriter, TemperatureReader, TemperatureWriter}
import org.apache.logging.log4j.Logger

case class RecordTemperature(input: TemperatureReader, output: TemperatureWriter, log: Logger) extends Runnable {
  def onError(log: Logger): Error => Unit = error => log.error(error.toString)

  def run(): Unit = {
    input.read.fold(onError(log), measurement => {
      output.write(measurement).leftMap(onError(log)); ()
    })
  }
}

/**
  * Tries to fix the problem where multiple measurements have the same time by merging measurements taken at the same 
  * time to a single write to the RRD. 
  * 
  * There's still potential for problems if the [[AllTemperatures]] are drained but the first new measurement is taken 
  * at the same time as the last (just drained) value.
  */
case class RecordTemperatures(temperatures: AllTemperatures, output: FixedTimeMeasurementWriter, log: Logger) extends Runnable {
  def run(): Unit = {
    val measurements = FixedTimeMeasurement.measurementsAtPointInTime(temperatures.drain())
    measurements.foreach(measurement =>
      output.write(measurement).leftMap(error => log.error(s"Error writing data from ${measurement.hosts.map(_.name).mkString(", ")}: ${error.toString}"))
    )
  }

}

