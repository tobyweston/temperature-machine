package bad.robot.temperature.task

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.Seconds.now
import bad.robot.temperature.{Error, Measurement, TemperatureReader, TemperatureWriter}
import org.apache.logging.log4j.Logger

case class RecordTemperature(host: Host, input: TemperatureReader, output: TemperatureWriter, log: Logger) extends Runnable {
  def onError(log: Logger): Error => Unit = error => log.error(error.toString)  
  
  def run(): Unit = {
    input.read.fold(onError(log), temperatures => {
      output.write(Measurement(host, now(), temperatures)).leftMap(onError(log)); ()
    })
  }
}
