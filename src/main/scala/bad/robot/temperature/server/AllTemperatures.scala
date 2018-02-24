package bad.robot.temperature.server

import java.time.temporal.ChronoUnit.{SECONDS => seconds}

import bad.robot.temperature.Measurement
import bad.robot.temperature.rrd.Seconds

object AllTemperatures {
  def apply(): AllTemperatures = new AllTemperatures()
}

class AllTemperatures {

  private var measurements: List[Measurement] = List()

  def put(measurement: Measurement) = {
    synchronized {
      measurements = measurement :: measurements
    }
  }

  def drain(): List[Measurement] = {
    synchronized {
      val copy = measurements
      measurements = List()
      copy
    }
  }
  
  def drainPartially(clock: => Seconds): List[Measurement] = {
    synchronized {
      val (older, recent) = measurements.partition(_.time.isBefore(clock.toInstant.minus(5, seconds)))
      measurements = recent
      older
    }
  }
}
