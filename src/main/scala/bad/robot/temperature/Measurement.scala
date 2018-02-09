package bad.robot.temperature

import bad.robot.temperature.rrd.{Host, Seconds}
import io.circe.Decoder.Result
import io.circe._

object Measurement {
  implicit def jsonEncoder: Encoder[Measurement] = new Encoder[Measurement] {
    def apply(measurement: Measurement): Json = Json.obj(
      ("host",    Encoder[Host].apply(measurement.host)),
      ("seconds", Json.fromLong(measurement.time.value)),
      ("sensors", Encoder[List[SensorReading]].apply(measurement.temperatures))
    )
  }

  implicit def jsonDecoder: Decoder[Measurement] = new Decoder[Measurement] {
    override def apply(cursor: HCursor): Result[Measurement] = for {
      host    <- cursor.get[Host]("host")
      seconds <- cursor.get[Long]("seconds")
      sensors <- cursor.get[List[SensorReading]]("sensors")
    } yield Measurement(host, Seconds(seconds), sensors)
  }
}

case class Measurement(host: Host, time: Seconds, temperatures: List[SensorReading])
