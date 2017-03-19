package bad.robot.temperature.server

import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress, Socket => _}

import bad.robot.temperature.server.DiscoveryServer.{Quit, ServerAddressRequestMessage, ServerAddressResponseMessage, UnknownRequestResponseMessage}
import bad.robot.temperature.server.Socket._
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scalaz.\/-

class DiscoveryServerTest extends Specification {

  "When the discovery server is started" >> {
    val server = new Thread(new DiscoveryServer())
    server.start()

    "Server will respond with it's address once pinged" >> {
      val socket = new DatagramSocket()
      pingServerOn(ServerAddressRequestMessage, socket)

      val response = socket.await(30 seconds).map(server => (server.getAddress.getHostAddress, server.payload))
      socket.close()
      response must be_\/-
      response match {
        case \/-((ip, message)) if ip == InetAddress.getLocalHost.getHostAddress => message must_== ServerAddressResponseMessage
        case \/-((ip, message)) if ip == "127.0.0.1" || ip == "127.0.1.1"        => message must_== ServerAddressResponseMessage
        case \/-((ip, _))                                                        => throw new Exception(s"$ip wasn't expected")
        case _                                                                   => throw new Exception("specs2 is a dick")
      }
    }

    "Server response when unknown message is received" >> {
      val socket = new DatagramSocket()
      pingServerOn("UNKNOWN_MESSAGE", socket)

      val response = socket.await(30 seconds).map(_.payload)
      socket.close()
      response must be_\/-(UnknownRequestResponseMessage)
    }

    step {
      pingServerOn(Quit, new DatagramSocket())
    }
  }

  private def pingServerOn(message: String, socket: DatagramSocket) {
    val server = new InetSocketAddress(InetAddress.getLocalHost, 8888)
    val data = message.getBytes
    socket.send(new DatagramPacket(data, data.length, server))
  }

}
