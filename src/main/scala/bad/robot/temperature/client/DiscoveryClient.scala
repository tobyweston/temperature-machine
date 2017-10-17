package bad.robot.temperature.client

import java.net.{DatagramPacket, DatagramSocket, InetAddress, NetworkInterface, Socket => _}

import bad.robot.temperature.Log
import bad.robot.temperature.server.DatagramPacketOps
import bad.robot.temperature.server.DiscoveryServer._
import bad.robot.temperature.server.Socket._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

object DiscoveryClient {

  def discover: InetAddress = {
    val socket = new DatagramSocket()
    socket.setBroadcast(true)

    allBroadcastAddresses.foreach(ping(_, socket))

    Log.info("Awaiting discovery server...")
    socket.await(30 seconds).fold(error => {
      Log.error(error.message)
      retry
    }, sender => {
      sender.payload match {
        case ServerAddressResponseMessage => sender.getAddress
        case _                            => retry
      }
    })
  }

  private def retry = {
    Thread.sleep((30 seconds).toMillis)
    discover
  }

  private def allNetworkInterfaces: List[NetworkInterface] = {
    NetworkInterface.getNetworkInterfaces
      .asScala
      .toList
      .filter(_.isUp)
      .filterNot(_.isLoopback)
  }

  private val broadcastAddresses: (NetworkInterface) => List[InetAddress] = (interface) => {
    interface.getInterfaceAddresses.asScala.toList.map(_.getBroadcast).filter(_ != null)
  }

  def allBroadcastAddresses: List[InetAddress] = {
    InetAddress.getByName(LocalNetworkBroadcastAddress) :: allNetworkInterfaces.flatMap(broadcastAddresses)
  }

  def ping: (InetAddress, DatagramSocket) => Unit = (address, socket) => {
    try {
      val data = ServerAddressRequestMessage.getBytes
      Log.info(s"Sending ping request to $address")
      socket.send(new DatagramPacket(data, data.length, address, ServerPort))
    } catch {
      case e: Throwable => Log.error(s"An error occurred pinging server. ${e.getMessage}")
    }
  }
}