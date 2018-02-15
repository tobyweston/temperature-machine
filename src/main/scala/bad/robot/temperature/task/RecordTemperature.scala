package bad.robot.temperature.task

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
