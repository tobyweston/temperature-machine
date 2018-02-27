package bad.robot.temperature.server

import java.time.{Clock, Instant, ZoneId}

import bad.robot.temperature._
import bad.robot.temperature.rrd.{Host, Seconds}
import org.specs2.mutable.Specification

class TemperaturesTest extends Specification {

  "Filter out old temperatures" >> {
    val measurement1 = Measurement(Host("lounge"), Seconds(0), List(
      SensorReading("28-00000dfg34ca", Temperature(31.1)),
      SensorReading("28-00000f33fdc3", Temperature(32.8))
    ))
    val measurement2 = Measurement(Host("bedroom"), Seconds(120), List(
      SensorReading("28-00000f3554ds", Temperature(21.1)),
      SensorReading("28-000003dd3433", Temperature(22.8))
    ))

    val temperatures = CurrentTemperatures(fixedClock(Instant.ofEpochSecond(300)))
    temperatures.updateWith(measurement1)
    temperatures.updateWith(measurement2)

    val expected = Map(
      Host("bedroom", None) -> Measurement(Host("bedroom"), Seconds(120), List(
        SensorReading("28-00000f3554ds", Temperature(21.1)),
        SensorReading("28-000003dd3433", Temperature(22.8))
      ))
    )

    temperatures.all must_== expected
  }

  "Filter out old temperatures when averaging" >> {
    val measurement1 = Measurement(Host("lounge"), Seconds(0), List(
      SensorReading("28-00000dfg34ca", Temperature(31.1)),
      SensorReading("28-00000f33fdc3", Temperature(32.8))
    ))
    val measurement2 = Measurement(Host("bedroom"), Seconds(60), List(
      SensorReading("28-00000f3554ds", Temperature(21.1)),
      SensorReading("28-000003dd3433", Temperature(22.8))
    ))

    val temperatures = CurrentTemperatures(fixedClock(Instant.ofEpochSecond(300)))
    temperatures.updateWith(measurement1)
    temperatures.updateWith(measurement2)

    val expected = Map(
      Host("bedroom", None) -> Measurement(Host("bedroom"), Seconds(60), List(
        SensorReading("Average", Temperature(21.950000000000003))
      ))
    )

    temperatures.average must_== expected
  }
  
  
  def fixedClock(instant: Instant = Instant.now) = Clock.fixed(instant, ZoneId.systemDefault())

}
