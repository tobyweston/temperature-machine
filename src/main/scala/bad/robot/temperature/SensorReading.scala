package bad.robot.temperature

import io.circe._

object SensorReading {
  implicit def jsonEncoder: Encoder[SensorReading] = new Encoder[SensorReading] {
    def apply(sensor: SensorReading): Json = Json.obj(
      ("name", Json.fromString(sensor.name)),
      ("temperature", Temperature.encoder(sensor.temperature))
    )
  }

  implicit def jsonDecoder: Decoder[SensorReading] = {
    Decoder(cursor => for {
      name        <- cursor.get[String]("name")
      temperature <- cursor.get[Temperature]("temperature")
    } yield SensorReading(name, temperature))
  }

  implicit class ListSensorTemperatureOps(temperatures: List[SensorReading]) {
    def average: SensorReading = temperatures match {
      case Nil           => SensorReading("Unknown", Temperature(0.0))
      case sensor :: Nil => SensorReading(sensor.name, sensor.temperature)
      case _             => SensorReading("Average", temperatures.map(_.temperature).reduce(_ + _) / temperatures.length)
    }
  }
}

case class SensorReading(name: String, temperature: Temperature)
