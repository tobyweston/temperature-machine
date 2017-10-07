package bad.robot.temperature

class SpikeIgnoringWriter(delegate: TemperatureWriter) extends TemperatureWriter {

  def write(measurement: Measurement) = {
    delegate.write(measurement)
  }

}
