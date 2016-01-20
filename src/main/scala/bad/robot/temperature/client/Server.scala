package bad.robot.temperature.client

import java.net.{DatagramPacket, DatagramSocket, InetAddress, NetworkInterface, Socket => _}

import bad.robot.temperature.client.Server._
import bad.robot.temperature.server.DiscoveryServer._
import bad.robot.temperature.server.Socket

import scala.collection.JavaConversions._

class Server {

  def discover() {
    implicit val socket = new DatagramSocket()
    socket.setBroadcast(true)

    allBroadcastAddresses.foreach(ping(_, socket))

    Socket.await(ServerAddressResponseMessage)(response => {
      println(s"Server address found: ${response.getAddress}")
    })
  }
}

object Server {

  def allNetworkInterfaces: List[NetworkInterface] = {
    NetworkInterface.getNetworkInterfaces
      .toList
      .filter(_.isUp)
      .filterNot(_.isLoopback)
  }

  val broadcastAddresses: (NetworkInterface) => List[InetAddress] = (interface) => {
    interface.getInterfaceAddresses.toList.map(_.getBroadcast).filter(_ != null)
  }

  def allBroadcastAddresses: List[InetAddress] = {
    InetAddress.getByName(LocalNetworkBroadcastAddress) :: allNetworkInterfaces.flatMap(broadcastAddresses)
  }

  def ping: (InetAddress, DatagramSocket) => Unit = (address, socket) => {
    try {
      println(s"sending request $address")
      val data = ServerAddressRequestMessage.getBytes
      socket.send(new DatagramPacket(data, data.length, address, ServerPort))
    } catch {
      case e: Throwable => System.err.println(s"An error occurred discovering server. This remote machine may not be able to publish data to the server. ${e.getMessage}")
    }
  }
}

object TestClient extends App {
  println("finding server...")
  new Server().discover()
  println("done")
}