package bad.robot.temperature.task

import bad.robot.temperature.Measurement
import bad.robot.temperature.rrd.Seconds

object FixedTimeMeasurement {
  def measurementsAtPointInTime(temperatures: List[Measurement]): List[FixedTimeMeasurement] = {
    temperatures
      .groupBy(measurement => measurement.time)
      .map { case (time, measurements) => FixedTimeMeasurement(time, measurements)}
      .toList
      .sorted(timeAscending)
  }

  private def timeAscending: Ordering[FixedTimeMeasurement] = (x: FixedTimeMeasurement, y: FixedTimeMeasurement) => x.time.compareTo(y.time)

}

case class FixedTimeMeasurement(time: Seconds, measurements: List[Measurement]) {
  def hosts = measurements.map(_.host).toSet
}