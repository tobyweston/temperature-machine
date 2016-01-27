package bad.robot.temperature.server

import java.net.{DatagramPacket, DatagramSocket}

import bad.robot.temperature.server.DiscoveryServer._

object Socket {

  def await(matchMessage: String)(onSuccess: (DatagramPacket) => Unit)(implicit socket: DatagramSocket) = {
    val packet = new DatagramPacket(new Array[Byte](BufferSize), BufferSize)
    print("Waiting for data...")
    socket.receive(packet)
    println(s"received from ${packet.getAddress}.")
    if (new String(packet.getData).trim == matchMessage) {
      onSuccess(packet)
    }
  }
}
