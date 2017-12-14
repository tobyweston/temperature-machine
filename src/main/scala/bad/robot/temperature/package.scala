package bad.robot

import java.io.{PrintWriter, StringWriter}

import argonaut.Argonaut._
import argonaut._
import org.http4s.argonaut.{jsonEncoderWithPrinterOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import scalaz.\/
import scalaz.syntax.either.ToEitherOps

package object temperature {

  implicit def http4sArgonautDecoder[A: DecodeJson]: EntityDecoder[A] = jsonOf[A]
  
  implicit def http4sArgonautEncoder[A: EncodeJson]: EntityEncoder[A] = jsonEncoderWithPrinterOf[A](PrettyParams.spaces2)
  
  def encode[A: EncodeJson](a: A): Json = a.jencode

  def decode[A: DecodeJson](value: String): Error \/ A = {
    value.decodeWith[Error \/ A, A](
      _.right,
      ParseError(_).left,
      (message, history) => {
        val detailMessage =
          if (history.toList.nonEmpty) s"$message Cursor history: $history"
          else message
        ParseError(detailMessage).left[A]
      })
  }

  def stackTraceOf(error: Throwable): String = {
    val writer = new StringWriter()
    error.printStackTrace(new PrintWriter(writer))
    writer.toString
  }
}
