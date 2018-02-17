package bad.robot.temperature.task

import bad.robot.temperature.server.AllTemperatures
import bad.robot.temperature.{Error, Measurement, TemperatureReader, TemperatureWriter}
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
  * Doesn't fix the problem where multiple measurements have the same time. 
  * 
  * Could also smash measurements taken at the same time together or filter out those that are within a second of each other
  */
case class RecordTemperatures(temperatures: AllTemperatures, output: TemperatureWriter, log: Logger) extends Runnable {
  def run(): Unit = {
    temperatures.drain().sorted(timeAscending).foreach(measurement =>
      output.write(measurement).leftMap(error => log.error(s"${measurement.host} ${error.toString}"))
    )
  }

  private def timeAscending: Ordering[Measurement] = (x: Measurement, y: Measurement) => x.time.compareTo(y.time)
}

