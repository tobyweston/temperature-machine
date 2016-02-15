package bad.robot.temperature

import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.task._

import scala.language.postfixOps

object Main extends App {

  findSensorsAndExecute(Tasks.application)

}
