package bad.robot.temperature

import bad.robot.temperature.rrd.{Host, Seconds}
import org.specs2.mutable.Specification

import scalaz.{\/, \/-}

class SpikeIgnoreWriterTest extends Specification {

  "Delegates" >> {
    val delegate = new StubWriter
    new SpikeIgnoringWriter(delegate).write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    delegate.lastValue must beSome
  }

  class StubWriter extends TemperatureWriter {
    var lastValue: Option[Measurement] = None

    def write(measurement: Measurement): \/[Error, Unit] = {
      lastValue = Some(measurement)
      \/-(())
    }
  }

}
