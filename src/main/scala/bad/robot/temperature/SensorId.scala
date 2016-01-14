package bad.robot.temperature

import java.util.concurrent.atomic.AtomicInteger

object SensorId {
  val ordinal = new AtomicInteger(1)
}

case class SensorId(value: String) {
  val ordinal = SensorId.ordinal.getAndIncrement()
}
