package bad.robot.temperature.rrd

import java.net.InetAddress

object Host {
  def name = Host(InetAddress.getLocalHost.getHostName)
}

case class Host(name: String) {

  // another arbitrary constraint of rrd4j; data source names can only be a max of 20 characters
  def trim(): Host = Host(name.take(20 - "-sensor-1".length))
}