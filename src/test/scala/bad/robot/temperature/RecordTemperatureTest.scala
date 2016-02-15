package bad.robot.temperature

import java.io.{ByteArrayOutputStream, PrintStream}

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.RecordTemperature
import org.specs2.mutable.Specification

import scalaz.{-\/, \/-}

class RecordTemperatureTest extends Specification {

  "Take a measurement and write it out" >> {
    val input = new TemperatureReader {
      def read = \/-(List(Temperature(69.9)))
    }
    val output = new TemperatureWriter {
      var temperatures = List[Temperature]()
      def write(measurement: Measurement) = {
        this.temperatures = measurement.temperatures
        \/-(Unit)
      }
    }
    RecordTemperature(Host("example"), input, output).run()
    output.temperatures must_== List(Temperature(69.9))
  }

  "Fail to take a measurement" >> {
    val input = new TemperatureReader {
      def read = -\/(UnexpectedError("whatever"))
    }
    val output = new TemperatureWriter {
      def write(measurement: Measurement) = ???
    }
    val log = new ByteArrayOutputStream()
    val error = new PrintStream(log)
    RecordTemperature(Host("example"), input, output, error).run()
    log.toString must contain("UnexpectedError(whatever)")
  }

  "Take a measurement but fail to write it" >> {
    val input = new TemperatureReader {
      def read = \/-(List(Temperature(69.9)))
    }
    val output = new TemperatureWriter {
      def write(measurement: Measurement) = -\/(UnexpectedError("whatever"))
    }
    val log = new ByteArrayOutputStream()
    val error = new PrintStream(log)
    RecordTemperature(Host("example"), input, output, error).run()
    log.toString must contain("UnexpectedError(whatever)")
  }

}
