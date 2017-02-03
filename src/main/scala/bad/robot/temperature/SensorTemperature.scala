package bad.robot.temperature

import argonaut.EncodeJson
import argonaut._
import Argonaut._

object SensorTemperature {
  implicit def sensorTemperatureEncoder: EncodeJson[SensorTemperature] = {
    EncodeJson((sensor: SensorTemperature) =>
      argonaut.Json(
        "name"        := sensor.name,
        "temperature" := sensor.temperature
      )
    )
  }

  implicit def sensorTemperatureDecoder: DecodeJson[SensorTemperature] = {
    DecodeJson(cursor => for {
      name        <- cursor.get[String]("name")
      temperature <- cursor.get[Temperature]("temperature")
    } yield SensorTemperature(name, temperature))
  }

  implicit class ListSensorTemperatureOps(temperatures: List[SensorTemperature]) {
    def average: SensorTemperature = {
      if (temperatures.isEmpty) SensorTemperature("Unknown", Temperature(0.0))
      else SensorTemperature("Average", temperatures.map(_.temperature).reduce(_ + _) / temperatures.length)
    }
  }
}

case class SensorTemperature(name: String, temperature: Temperature) {

}
