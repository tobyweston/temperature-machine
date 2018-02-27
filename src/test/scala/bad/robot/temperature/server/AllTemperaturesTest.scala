package bad.robot.temperature.server

import bad.robot.temperature.rrd.{Host, Seconds}
import bad.robot.temperature.{Measurement, SensorReading, Temperature}
import org.specs2.mutable.Specification

class AllTemperaturesTest extends Specification {

  "drain's partially based on time (the most recent 5 seconds are retained)" >> {
    val temperatures = AllTemperatures()
    temperatures.put(measurement("A", 0, 20.0, 20.1))
    temperatures.put(measurement("B", 1, 21.0, 21.1))
    temperatures.put(measurement("C", 2, 22.0, 22.1))
    temperatures.put(measurement("D", 3, 23.2, 23.3))
    temperatures.put(measurement("E", 4, 24.0, 24.1))
    temperatures.put(measurement("F", 5, 25.0, 25.1))
    temperatures.put(measurement("G", 6, 26.0, 26.1))
    temperatures.put(measurement("H", 7, 27.0, 27.1))
    temperatures.put(measurement("I", 8, 28.0, 28.1))
    temperatures.put(measurement("J", 9, 29.0, 29.1))


    temperatures.drainPartially(Seconds(10)) must containTheSameElementsAs(List(
      measurement("A", 0, 20.0, 20.1),
      measurement("B", 1, 21.0, 21.1),
      measurement("C", 2, 22.0, 22.1),
      measurement("D", 3, 23.2, 23.3),
      measurement("E", 4, 24.0, 24.1)
    ))

    temperatures.drainPartially(Seconds(20)) must containTheSameElementsAs(List(
      measurement("F", 5, 25.0, 25.1),
      measurement("G", 6, 26.0, 26.1),
      measurement("H", 7, 27.0, 27.1),
      measurement("I", 8, 28.0, 28.1),
      measurement("J", 9, 29.0, 29.1)
    ))

    temperatures.drainPartially(Seconds(30)) must_== List()
  }

  private def measurement(host: String, time: Long, temperatures: Double*) = {
    Measurement(Host(host), Seconds(time), temperatures.zipWithIndex.map { case (temperature, index) =>
      SensorReading(s"sensor-$index", Temperature(temperature))
    }.toList)
  }
}
