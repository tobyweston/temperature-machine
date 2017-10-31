package bad.robot

import java.io.{PrintWriter, StringWriter}

import argonaut.Argonaut._
import argonaut._
import bad.robot.temperature.rrd.{RrdFile, _}
import org.http4s.argonaut.{jsonEncoderWithPrinterOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.slf4j.LoggerFactory

import scalaz.\/
import scalaz.syntax.either.ToEitherOps

package object temperature {

  sys.props +=
    ("org.slf4j.simpleLogger.logFile"                   ->  RrdFile.path / "temperature-machine.log") +=
    ("org.slf4j.simpleLogger.defaultLogLevel"           -> "error") +=
    ("org.slf4j.simpleLogger.log.bad.robot.temperature" -> "info") +=
    ("org.slf4j.simpleLogger.showDateTime"              -> "true") +=
    ("org.slf4j.simpleLogger.dateTimeFormat"            -> "\u0000yyyy-MM-dd HH:mm:ss:SSS") +=
    ("org.slf4j.simpleLogger.showThreadName"            -> "true") +=
    ("org.slf4j.simpleLogger.showLogName"               -> "false") +=
    ("org.slf4j.simpleLogger.showShortLogName"          -> "false")

  val Log = LoggerFactory.getLogger("bad.robot.temperature")
  
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
