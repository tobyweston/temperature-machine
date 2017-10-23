package bad.robot.temperature

import java.time.Instant

case class LogMessage(time: Instant, thread: String, level: String, message: String)
