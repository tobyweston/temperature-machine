package bad.robot

import argonaut.Argonaut._
import argonaut._

import scalaz.\/
import scalaz.syntax.either.ToEitherOps

package object temperature {

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

  type Ip = String
}
