package bad.robot.temperature

import bad.robot.temperature.rrd.{Host, Seconds}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import scala.Double._
import scalaz.{\/, \/-}

class ErrorOnTemperatureSpikeTest extends Specification with AfterAll {

  val SensorError: PartialFunction[Error, MatchResult[Any]] = {
    case _: SensorSpikeError => ok
  }

  "Delegates" >> {
    val delegate = new StubWriter
    new ErrorOnTemperatureSpike(delegate).write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1))))
    )
  }

  "Errors on spiked value (single sensor)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.4)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6)))))
    writer.write(Measurement(Host("example"), Seconds(4), List(SensorReading("A", Temperature(51.1))))) must be_-\/.like(SensorError)
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))),
      Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.4)))),
      Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6))))
    )
  }

  "Error on spiked values (multiple sensors)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6)))))
    writer.write(Measurement(Host("example"), Seconds(5), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(7), List(SensorReading("A", Temperature(51.6))))) must be_-\/.like(SensorError)
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("B", Temperature(31.4)))))
    writer.write(Measurement(Host("example"), Seconds(4), List(SensorReading("B", Temperature(31.1)))))
    writer.write(Measurement(Host("example"), Seconds(6), List(SensorReading("B", Temperature(31.4)))))
    writer.write(Measurement(Host("example"), Seconds(8), List(SensorReading("B", Temperature(51.1))))) must be_-\/.like(SensorError)
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))),
      Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6)))),
      Measurement(Host("example"), Seconds(5), List(SensorReading("A", Temperature(21.1)))),
      Measurement(Host("example"), Seconds(2), List(SensorReading("B", Temperature(31.4)))),
      Measurement(Host("example"), Seconds(4), List(SensorReading("B", Temperature(31.1)))),
      Measurement(Host("example"), Seconds(6), List(SensorReading("B", Temperature(31.4))))
    )
  }

  "Recovers from spiked value (single sensor)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.4)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6)))))
    writer.write(Measurement(Host("example"), Seconds(4), List(SensorReading("A", Temperature(51.1))))) must be_-\/.like(SensorError)
    writer.write(Measurement(Host("example"), Seconds(5), List(SensorReading("A", Temperature(51.2))))) must be_-\/.like(SensorError)
    writer.write(Measurement(Host("example"), Seconds(6), List(SensorReading("A", Temperature(51.5))))) must be_-\/.like(SensorError)
    writer.write(Measurement(Host("example"), Seconds(7), List(SensorReading("A", Temperature(21.7)))))
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))),
      Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.4)))),
      Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6)))),
      Measurement(Host("example"), Seconds(7), List(SensorReading("A", Temperature(21.7))))
    )
  }

  "Negative spikes values (single sensor)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.4)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6)))))
    writer.write(Measurement(Host("example"), Seconds(4), List(SensorReading("A", Temperature(1.1))))) must be_-\/.like(SensorError)
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))),
      Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.4)))),
      Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(21.6))))
    )
  }

  "Negative spikes values (single sensor), example from production" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate, Barrier(1))
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(0.5)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(0.5)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(0.4))))) 
    writer.write(Measurement(Host("example"), Seconds(4), List(SensorReading("A", Temperature(-0.7))))) must be_-\/.like(SensorError)
    writer.write(Measurement(Host("example"), Seconds(5), List(SensorReading("A", Temperature(0.4)))))
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(0.5)))),
      Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(0.5)))),
      Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(0.4)))),
      Measurement(Host("example"), Seconds(5), List(SensorReading("A", Temperature(0.4))))
    )
  }
  
  "NaN (32.625 - 0.0 / 0.0 * 100 is NaN)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(0.0)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(32.625))))) must be_-\/.like(SensorError)
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(0.0))))
    )
  }

  "Infinity (0.0 - 32.625 / 0.0 * 100 is -Infinity)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(32.625)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(0.0))))) must be_-\/.like(SensorError)
    delegate.temperatures must_== List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(32.625))))
    )
  }

  "Error message on a spiked value (single sensor)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.6)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(51.1))))) must be_-\/.like {
      case e: SensorSpikeError => e.message must_==
        """An unexpected spike was encountered on:
          | sensor(s)             : A
          | previous temperatures : 21.6 °C
          | spiked temperatures   : 51.1 °C
          |""".stripMargin
    }
  }

  "Error message on a spiked value (multiple sensors)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A1", Temperature(21.1)), SensorReading("A2", Temperature(21.3)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("B1", Temperature(21.6)), SensorReading("B2", Temperature(21.8)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A1", Temperature(51.4)), SensorReading("A2", Temperature(51.1))))) must be_-\/.like {
      case e: SensorSpikeError => e.message must_==
        """An unexpected spike was encountered on:
          | sensor(s)             : A1, A2
          | previous temperatures : 21.1 °C, 21.3 °C
          | spiked temperatures   : 51.4 °C, 51.1 °C
          |""".stripMargin
    }
  }
  
  "What happens with NaN (let it through)" >> {
    val delegate = new StubWriter
    val writer = new ErrorOnTemperatureSpike(delegate)
    writer.write(Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))))
    writer.write(Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.6)))))
    writer.write(Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(NaN))))) must be_\/- 
    writer.write(Measurement(Host("example"), Seconds(4), List(SensorReading("A", Temperature(21.8)))))
    delegate.temperatures must containAllOf(List(
      Measurement(Host("example"), Seconds(1), List(SensorReading("A", Temperature(21.1)))),
      Measurement(Host("example"), Seconds(2), List(SensorReading("A", Temperature(21.6)))),
//      Measurement(Host("example"), Seconds(3), List(SensorReading("A", Temperature(NaN)))), // can't do equality check on NaN
      Measurement(Host("example"), Seconds(4), List(SensorReading("A", Temperature(21.8))))
    ))
  }
  
  "Toggle the use based on system property" >> {
    ErrorOnTemperatureSpike(new StubWriter()) must haveClass[StubWriter]
    sys.props += ("avoid.spikes" -> "true")
    ErrorOnTemperatureSpike(new StubWriter()) must haveClass[ErrorOnTemperatureSpike]
  }

  class StubWriter extends TemperatureWriter {
    var temperatures: List[Measurement] = List()

    def write(measurement: Measurement): \/[Error, Unit] = {
      temperatures = temperatures :+ measurement
      \/-(())
    }
  }

  override def afterAll(): Unit = sys.props -= "avoid.spikes"

}