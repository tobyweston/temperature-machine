package bad.robot

import argonaut.Argonaut._
import argonaut._
import scalaz.syntax.show._

import scalaz.{-\/, \/-, \/}

package object temperature {

  def encode[A: EncodeJson](a: A): Json = a.jencode

  def decode[A: DecodeJson](value: String): Error \/ A = {
    value.decodeWith[Error \/ A, A](json => {
      \/-(json)
    }, failure => {
      -\/(ParseError(failure))
    }, (message, history) => {
      val detailMessage = if (history.toList.nonEmpty) s"$message Cursor history: ${history.shows}" else message
      -\/(ParseError(detailMessage))
    })
  }
}
