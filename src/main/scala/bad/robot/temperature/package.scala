package bad.robot

package object temperature {

  implicit def readingToTemperature(reading: (SensorId, Temperature)): Temperature = reading._2
  implicit def readingToSensorId(reading: (SensorId, Temperature)): SensorId = reading._1

}
