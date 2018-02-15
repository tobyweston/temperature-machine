package bad.robot.temperature.task

import bad.robot.temperature.server.Temperatures
import bad.robot.temperature.{Error, TemperatureReader, TemperatureWriter}
import org.apache.logging.log4j.Logger

case class RecordTemperature(input: TemperatureReader, output: TemperatureWriter, log: Logger) extends Runnable {
  def onError(log: Logger): Error => Unit = error => log.error(error.toString)  
  
  def run(): Unit = {
    input.read.fold(onError(log), measurement => {
      output.write(measurement).leftMap(onError(log)); ()
    })
  }
}

// Doesn't fix the problem where multiple measurements have the same time. Really need to drain these from a queue
case class RecordTemperatures(temperatures: Temperatures, output: TemperatureWriter, log: Logger) extends Runnable {
  def run(): Unit = {
    temperatures.all.foreach { case (host, measurement) =>
      output.write(measurement).leftMap(error => log.error(s"${host.name} ${error.toString}")); ()
    }
  }
}

