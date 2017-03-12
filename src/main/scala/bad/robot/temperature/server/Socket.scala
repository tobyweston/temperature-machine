package bad.robot.temperature.server

import java.net.{DatagramPacket, DatagramSocket, SocketTimeoutException}

import bad.robot.temperature.server.DiscoveryServer._
import bad.robot.temperature.{Error, Timeout, UnexpectedError}

import scala.concurrent.duration.Duration
import scala.{Error => _}
import scalaz.\/

object Socket {

  implicit class SocketOps(socket: DatagramSocket) {
    def await(timeout: Duration = Duration(0, "seconds")): Error \/ DatagramPacket = {
      val packet = new DatagramPacket(new Array[Byte](BufferSize), BufferSize)
      socket.setSoTimeout(timeout.toMillis.toInt)
      \/.fromTryCatchNonFatal {
        socket.receive(packet)
        packet
      }.leftMap {
        case e: SocketTimeoutException => Timeout(s"socket timed out after $timeout ms")
        case e: Throwable              => UnexpectedError(e.getMessage)
      }
    }
  }

}
