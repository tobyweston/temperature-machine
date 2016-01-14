package bad.robot.temperature

import scala.util.Random
import scalaz.\/-

case class RandomTemperatures() extends TemperatureReader {

  val random = new Random()

  var current = random.nextInt(30) + random.nextDouble()

  def read = {
    if (random.nextDouble() > 0.5)
      current = current + random.nextDouble()
    else
      current = current - random.nextDouble()
    \/-(List(Temperature(current)))
  }
}
