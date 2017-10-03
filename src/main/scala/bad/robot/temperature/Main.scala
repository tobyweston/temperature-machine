package bad.robot.temperature

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.server.Server

object Main extends App {

  Server.main(Array(Host.local.name))

}
