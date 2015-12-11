package bad.robot.temperature

case class Temperature(raw: Double) {
  def celsius: Double = raw / 1000
  def fahrenheit: Double = celsius * 9 / 5 + 32
}

