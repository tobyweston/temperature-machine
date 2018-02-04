package bad.robot.temperature

import java.net.DatagramPacket

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._

import scalaz.\/

package object server {

  implicit class EitherToResponse[A](either: Error \/ A) {
    def toHttpResponse(success: A => IO[Response[IO]]): IO[Response[IO]] = {
      either.fold(errors, success)
    }

    val errors = PartialFunction[Error, IO[Response[IO]]] {
      case error @ CrcFailure()        => InternalServerError(error.message)
      case error @ FailedToFindFile(_) => NotFound(error.message)
      case error @ FileError(_)        => InternalServerError(error.message)
      case error @ SensorError(_)      => InternalServerError(error.message)
      case error @ SensorSpikeError(_) => InternalServerError(error.message)
      case error @ UnexpectedError(_)  => InternalServerError(error.message)
      case error @ Timeout(_)          => InternalServerError(error.message)
      case error @ ParseError(_)       => BadRequest(error.message)
      case error @ RrdError(_)         => BadGateway(error.message)
    }
  }

  implicit class DatagramPacketOps(packet: DatagramPacket) {
    def payload = new String(packet.getData).trim
  }

}
