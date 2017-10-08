package bad.robot.temperature

import java.lang.{Error => _}
import java.net.DatagramPacket

import org.http4s.Response
import org.http4s.dsl._

import scalaz.\/
import scalaz.concurrent.Task

package object server {

  implicit class EitherToResponse[A](either: Error \/ A) {
    def toHttpResponse(success: A => Task[Response]): Task[Response] = {
      either.fold(errors, success)
    }

    val errors = PartialFunction[Error, Task[Response]] {
      case error @ CrcFailure()              => InternalServerError(error.message)
      case error @ FailedToFindFile(_)       => NotFound(error.message)
      case error @ FileError(_)              => InternalServerError(error.message)
      case error @ SensorError(_)            => InternalServerError(error.message)
      case error @ SensorSpikeError(_, _, _) => InternalServerError(error.message)
      case error @ UnexpectedError(_)        => InternalServerError(error.message)
      case error @ Timeout(_)                => InternalServerError(error.message)
      case error @ ParseError(_)             => BadRequest(error.message)
      case error @ RrdError(_)               => BadGateway(error.message)
    }
  }

  implicit class DatagramPacketOps(packet: DatagramPacket) {
    def payload = new String(packet.getData).trim
  }

}
