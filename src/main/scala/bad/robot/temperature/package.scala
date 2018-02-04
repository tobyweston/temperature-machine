package bad.robot

import java.io.{File, PrintWriter, StringWriter}

import cats.effect.IO
import cats.syntax.either._
import io.circe._
import org.http4s.circe.{jsonEncoderWithPrinterOf, jsonOf}
import io.circe.parser._
import org.http4s.{EntityDecoder, EntityEncoder}

import scalaz.\/
import scalaz.syntax.std.either._

package object temperature {

  def jsonDecoder[A: Decoder]: EntityDecoder[IO, A] = jsonOf[IO, A]
  def jsonEncoder[A: Encoder]: EntityEncoder[IO, A] = jsonEncoderWithPrinterOf(spaces2PlatformSpecific)

  def encode[A: Encoder](a: A): Json = Encoder[A].apply(a)

  // deprecated
  def decodeAsDisjunction[A: Decoder](value: String): temperature.Error \/ A = {
    decode(value)
      .leftMap(error => ParseError(error.getMessage))
      .disjunction
  }

  def stackTraceOf(error: Throwable): String = {
    val writer = new StringWriter()
    error.printStackTrace(new PrintWriter(writer))
    writer.toString
  }

  private val eol = sys.props("line.separator")
  val spaces2PlatformSpecific = Printer(
    preserveOrder = true
    , dropNullValues = false
    , indent = "  "
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
  )

  implicit class JsonOps(json: Json) {
    /** Pretty print with platform specific line endings, see [[https://github.com/argonaut-io/argonaut/issues/268]] **/
    def spaces2ps: String = spaces2PlatformSpecific.pretty(json)
  }

  implicit class FileOps(file: File) {
    def /(child: String): File = new File(file, child)
  }

}
