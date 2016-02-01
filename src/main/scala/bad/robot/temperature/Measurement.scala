package bad.robot.temperature

import argonaut._
import Argonaut._
import bad.robot.temperature.rrd.Seconds

object Measurement {
  implicit def measurementEncoder: EncodeJson[Measurement] = {
    EncodeJson((measurement: Measurement) =>
      argonaut.Json(
        "source" := measurement.source,
        "seconds" := measurement.time.value,
        "sensors" := measurement.temperatures
      )
    )
  }

  implicit def measurementDecoder: DecodeJson[Measurement] = {
    DecodeJson(cursor => for {
      source  <- cursor.get[String]("source")
      seconds <- cursor.get[Long]("seconds")
      sensors <- cursor.get[List[Temperature]]("sensors")
    } yield Measurement(source, Seconds(seconds), sensors))
  }
}

case class Measurement(source: String, time: Seconds, temperatures: List[Temperature])
