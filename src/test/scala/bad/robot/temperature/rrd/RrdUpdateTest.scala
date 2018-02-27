package bad.robot.temperature.rrd

import java.io.File

import bad.robot.temperature.task.FixedTimeMeasurement
import bad.robot.temperature.{Measurement, RrdError, SensorReading, Temperature}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration

class RrdUpdateTest extends Specification {

  sequential

  private val hosts: List[Host] = hosts("A", "B", "C")
  private val NoValues = List(
    // Host A
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    // Host B
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    // Host C
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0)),
    SensorReading("Unknown", Temperature(0))
  )

  "No hosts found" >> {
    val time = Seconds.now()
    val file = createTempRrd

    val update = RrdUpdate(hosts, file).apply(FixedTimeMeasurement(time, List(
      Measurement(Host("Z"), time, List(
        SensorReading("sensor-1", Temperature(19.8)),
        SensorReading("sensor-2", Temperature(19.9))
      ))
    )))
    update must be_\/-.like {
      case readings => readings.map(nanToZero) must_== NoValues
    }
  }

  "Single hosts temperatures" >> {
    val time = Seconds.now()
    val file = createTempRrd

    val update = RrdUpdate(hosts, file).apply(FixedTimeMeasurement(time, List(
      Measurement(Host("A"), time, List(
        SensorReading("sensor-1", Temperature(19.8)),
        SensorReading("sensor-2", Temperature(19.9))
      ))
    )))
    update must be_\/-.like {
      case readings => readings.map(nanToZero) must_== List(
        SensorReading("sensor-1", Temperature(19.8)),
        SensorReading("sensor-2", Temperature(19.9)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        // Host B
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        // Host C
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0))
      )
    }
  }

  "Multiple hosts temperatures" >> {
    val time = Seconds.now()
    val file = createTempRrd

    val update = RrdUpdate(hosts, file).apply(FixedTimeMeasurement(time, List(
      Measurement(Host("A"), time, List(
        SensorReading("sensor-1", Temperature(19.8)),
        SensorReading("sensor-2", Temperature(19.9))
      )),
      Measurement(Host("Z"), time, List(
        SensorReading("sensor-5", Temperature(14.8)),
        SensorReading("sensor-6", Temperature(14.9))
      )),
      Measurement(Host("C"), time, List(
        SensorReading("sensor-3", Temperature(16.8)),
        SensorReading("sensor-4", Temperature(16.9))
      ))
    )))
    update must be_\/-.like {
      case readings => readings.map(nanToZero) must_== List(
        SensorReading("sensor-1", Temperature(19.8)),
        SensorReading("sensor-2", Temperature(19.9)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        // Host B
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        // Host C
        SensorReading("sensor-3", Temperature(16.8)),
        SensorReading("sensor-4", Temperature(16.9)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0)),
        SensorReading("Unknown", Temperature(0))
      )
    }
  }
  
  "Error on RRD" >> {
    val update = RrdUpdate(hosts, new File("doesnt.exist")).apply(FixedTimeMeasurement(Seconds(0), List(
      Measurement(Host("A"), Seconds(0), List(
        SensorReading("sensor-1", Temperature(19.8)),
        SensorReading("sensor-2", Temperature(19.9))
      ))
    )))
    update must be_-\/.like {
      case RrdError(message) => message must contain("Could not open")
    }
  }

  private def createTempRrd = {
    val file = File.createTempFile("test", ".rrd")
    RrdFile(hosts, Duration(30, "seconds"), file).create(Seconds(0))
    file
  }

  private def hosts(names: String*) = names.map(Host(_, None)).toList

  private val nanToZero: SensorReading => SensorReading = reading =>
    reading.copy(temperature = if (reading.temperature.celsius.isNaN) Temperature(0) else reading.temperature)

}
