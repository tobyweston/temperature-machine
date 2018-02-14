package bad.robot.temperature

import java.lang.Math._

case class Barrier(degrees: Degrees) {
  
  def breached(previous: Temperature, current: Temperature) = {
    abs(current.celsius - previous.celsius) > degrees
  }
}
