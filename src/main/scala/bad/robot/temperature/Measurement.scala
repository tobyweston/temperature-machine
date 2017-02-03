package bad.robot.temperature

import argonaut._
import Argonaut._
import bad.robot.temperature.rrd.{Host, Seconds}

object Measurement {
  implicit def measurementEncoder: EncodeJson[Measurement] = {
    EncodeJson((measurement: Measurement) =>
      argonaut.Json(
        "host"    := measurement.host.name,
        "seconds" := measurement.time.value,
        "sensors" := measurement.temperatures
      )
    )
  }

  implicit def measurementDecoder: DecodeJson[Measurement] = {
    DecodeJson(cursor => for {
      host    <- cursor.get[String]("host")
      seconds <- cursor.get[Long]("seconds")
      sensors <- cursor.get[List[SensorReading]]("sensors")
    } yield Measurement(Host(host), Seconds(seconds), sensors))
  }
}

case class Measurement(host: Host, time: Seconds, temperatures: List[SensorReading])
