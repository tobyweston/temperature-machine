package bad.robot.temperature

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.server.Server

import scala.language.postfixOps

object Main extends App {

  Server.main(Array(Host.local.name))

}
