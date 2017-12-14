package bad.robot.temperature.task

import java.io.{OutputStream, PrintStream}

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.Seconds.now
import bad.robot.temperature.{Error, Measurement, TemperatureReader, TemperatureWriter}
import org.slf4j.Logger

case class RecordTemperature(host: Host, input: TemperatureReader, output: TemperatureWriter, error: PrintStream = System.err) extends Runnable {
  def onError(stream: PrintStream): Error => Unit = error => stream.print(error + "\u0000")  
  
  def run(): Unit = {
    input.read.fold(onError(error), temperatures => {
      output.write(Measurement(host, now(), temperatures)).leftMap(onError(error)); ()
    })
  }
}

import java.io.ByteArrayOutputStream

object ErrorLogger {
  
  def apply(log: Logger = bad.robot.logging.Log): PrintStream = new PrintStream(new OutputStream {

    private val stream = new ByteArrayOutputStream(1000)

    def write(bytes: Int): Unit = {
      if (bytes == '\u0000') {
        val line = stream.toString
        stream.reset()
        log.error(line)
      } else {
        stream.write(bytes)
      }
    }
  })

}
