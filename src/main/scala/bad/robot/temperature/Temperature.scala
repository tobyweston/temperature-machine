package bad.robot.temperature

case class Temperature(celsius: Double) {
  def fahrenheit: Double = celsius * 9 / 5 + 32
  def +(other: Temperature) = new Temperature(this.celsius + other.celsius)
  def /(divisor: Int) = new Temperature(this.celsius / divisor)
}

