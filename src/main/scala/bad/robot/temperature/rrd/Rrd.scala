package bad.robot.temperature.rrd

import bad.robot.temperature.FixedTimeMeasurementWriter
import bad.robot.temperature.task.FixedTimeMeasurement

case class Rrd(monitored: List[Host]) extends FixedTimeMeasurementWriter {
  def write(measurement: FixedTimeMeasurement) = RrdUpdate(monitored).apply(measurement)
}