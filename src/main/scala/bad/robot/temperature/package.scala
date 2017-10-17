package bad.robot

import argonaut.Argonaut._
import argonaut._
import org.slf4j.LoggerFactory

import scalaz.\/
import scalaz.syntax.either.ToEitherOps

package object temperature {

//  sys.props += 
//    ("org.slf4j.simpleLogger.showShortLogName" -> "true") +=
//    ("org.slf4j.simpleLogger.dateTimeFormat=" -> "yyyy-MM-dd HH:mm:ss") +=
//  ("org.slf4j.simpleLogger.showDateTime" -> "true")

  val Log = LoggerFactory.getLogger("bad.robot.temperature")
  
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

}
