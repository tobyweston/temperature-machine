package bad.robot.temperature.server

import bad.robot.temperature.Measurement

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
}
