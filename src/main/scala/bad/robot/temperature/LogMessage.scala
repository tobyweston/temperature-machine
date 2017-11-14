package bad.robot.temperature

import java.time.Instant

import argonaut.Argonaut._
import argonaut.{CodecJson, Json}

object LogMessage {
  
  implicit val instantCodec: CodecJson[Instant] =
    CodecJson(
      (time: Instant) =>
        Json("instant" -> jString(time.toString)),
      cursor => for {
        time <- cursor.get[String]("instant")
      } yield Instant.parse(time)
    )
  
  implicit val codec = CodecJson.derive[LogMessage]
}

case class LogMessage(time: Instant, thread: String, level: String, message: String)
