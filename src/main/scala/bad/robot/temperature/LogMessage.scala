package bad.robot.temperature

import java.time.Instant

import cats.syntax.either._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

object LogMessage {

  implicit val decoderInstant: Decoder[Instant] = Decoder.decodeString.emap { string =>
    Either.catchNonFatal(Instant.parse(string)).leftMap(_ => "instant")
  }
  implicit val encoderInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

  implicit val encoderLogMessage: Encoder[LogMessage] = deriveEncoder[LogMessage]
  implicit val decoderLogMessage: Decoder[LogMessage] = deriveDecoder[LogMessage]
}

case class LogMessage(time: Instant, thread: String, level: String, message: String)
