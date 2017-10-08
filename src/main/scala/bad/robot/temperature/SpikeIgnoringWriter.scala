package bad.robot.temperature

import bad.robot.temperature.SpikeIgnoringWriter._

import scala.collection.concurrent.TrieMap

object SpikeIgnoringWriter {
  type SensorName = String

  private val thirtyPercent = 30

  // negative numbers would be a decrease, which we'll ignore (use Math.abs if we change our mind later)
  def percentageIncrease(oldValue: Double, newValue: Double): Double = (newValue - oldValue) / oldValue * 100
}

class SpikeIgnoringWriter(delegate: TemperatureWriter) extends TemperatureWriter {

  private val temperatures: TrieMap[SensorName, Temperature] = TrieMap()

  def write(measurement: Measurement) = {

//    val readings = measurement.temperatures.map(current => {
//      val previous = temperatures.getOrElseUpdate(current.name, current.temperature)
//      if (spikeBetween(current, previous)) {
//        current.copy(temperature = previous)  // use old value
//      } else {
//        temperatures.update(current.name, current.temperature)
//        current                               // pass down
//      }
//    })

    val readings2 = measurement.temperatures.map(current => {
      temperatures.get(current.name) match {
        case Some(previous) if spikeBetween(current, previous) => current.copy(temperature = previous)
        case _                                                 => temperatures.update(current.name, current.temperature); current
      }
    })

    delegate.write(measurement.copy(temperatures = readings2))
  }

  private def spikeBetween(reading: SensorReading, previous: Temperature) = {
    percentageIncrease(previous.celsius, reading.temperature.celsius) >= thirtyPercent
  }

}
