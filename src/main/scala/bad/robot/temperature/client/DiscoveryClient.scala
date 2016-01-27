package bad.robot.temperature.client

import java.net.{DatagramPacket, DatagramSocket, InetAddress, NetworkInterface, Socket => _}

import bad.robot.temperature.client.DiscoveryClient._
import bad.robot.temperature.server.DiscoveryServer._
import bad.robot.temperature.server.Socket._

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.language.postfixOps
import bad.robot.temperature.Error

class DiscoveryClient {

  def discover(onError: Error => Unit = error => println(error)): InetAddress = {
    val socket = new DatagramSocket()
    socket.setBroadcast(true)

    allBroadcastAddresses.foreach(ping(_, socket))

    println("Awaiting server...")
    socket.await(30 seconds).fold(error => {
      onError(error)
      discover(onError)
    }, sender => {
      sender.getAddress
    })
  }
}

object DiscoveryClient {

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
      val data = ServerAddressRequestMessage.getBytes
      println(s"Sending ping request to $address")
      socket.send(new DatagramPacket(data, data.length, address, ServerPort))
    } catch {
      case e: Throwable => System.err.println(s"An error occurred pinging server. ${e.getMessage}")
    }
  }
}

object TestClient extends App {
  println("Discover Client started, attempting to find server...")
  val server = new DiscoveryClient().discover()
  println(s"Server address found: $server")
}