package bad.robot

import java.io.{PrintWriter, StringWriter}

import argonaut.Argonaut._
import argonaut._
import cats.effect.IO
import org.http4s.argonaut.{jsonEncoderWithPrinterOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import scalaz.\/
import scalaz.syntax.either.ToEitherOps

package object temperature {

  implicit def http4sArgonautDecoder[A: DecodeJson]: EntityDecoder[IO, A] = jsonOf[IO, A]
  
  implicit def http4sArgonautEncoder[A: EncodeJson]: EntityEncoder[IO, A] = jsonEncoderWithPrinterOf[IO, A](spaces2PlatformSpecific)
  
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

  private val eol = sys.props("line.separator")
  val spaces2PlatformSpecific = PrettyParams(
    indent = "  "
    , lbraceLeft = ""
    , lbraceRight = eol
    , rbraceLeft = eol
    , rbraceRight = ""
    , lbracketLeft = ""
    , lbracketRight = eol
    , rbracketLeft = eol
    , rbracketRight = ""
    , lrbracketsEmpty = ""
    , arrayCommaLeft = ""
    , arrayCommaRight = eol
    , objectCommaLeft = ""
    , objectCommaRight = eol
    , colonLeft = " "
    , colonRight = " "
    , preserveOrder = false
    , dropNullKeys = false
  )

  implicit class JsonOps(json: Json) {
    /** Pretty print with platform specific line endings, see [[https://github.com/argonaut-io/argonaut/issues/268]] **/
    def spaces2ps: String = spaces2PlatformSpecific.pretty(json)
  }
}
