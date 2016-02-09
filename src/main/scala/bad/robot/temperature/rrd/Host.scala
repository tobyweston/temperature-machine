package bad.robot.temperature.rrd

import java.net.InetAddress

object Host {
  def name = Host(InetAddress.getLocalHost.getHostName)
}

case class Host(name: String)