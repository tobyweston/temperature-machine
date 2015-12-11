package bad.robot.temperature.ds18b20

import bad.robot.temperature.Temperature
import bad.robot.temperature.Error

import scalaz.{-\/, \/}

object Parser {
  def parse(content: String): Error \/ Temperature = {
    ???
  }
}

