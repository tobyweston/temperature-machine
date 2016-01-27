package bad.robot.temperature.server


import java.net._

import bad.robot.temperature.server.DiscoveryServer._

object DiscoveryServer {
  val LocalNetworkBroadcastAddress = "255.255.255.255"
  val ServerAddressRequestMessage = "SERVER_ADDRESS_REQUEST"
  val ServerAddressResponseMessage = "SERVER_ADDRESS_RESPONSE"
  val ServerPort = 8888
  val BufferSize = 15000
}

class DiscoveryServer extends Runnable {

  def run() {
    try {
      implicit val socket = new DatagramSocket(ServerPort, InetAddress.getByName("0.0.0.0"))
      socket.setBroadcast(true)

      while (!Thread.currentThread().isInterrupted) {
        println("Listening for broadcast messages...")

        Socket.await(ServerAddressRequestMessage)(response => {
          println("request received, sending response...")
          val data = ServerAddressResponseMessage.getBytes
          socket.send(new DatagramPacket(data, data.length, response.getAddress, response.getPort))
        })
      }
    } catch {
      case e: Throwable => System.err.println(s"An error occurred listening for server discovery messages. Remote machines may not be able to publish their data to the server. ${e.getMessage}")
    }
  }
}

object TestServer extends App {
  println("Starting Discovery Server...")
  new Thread(new DiscoveryServer(), "temperature-machine-discovery-server").start()
  println("Discovery Server stopped...")
}