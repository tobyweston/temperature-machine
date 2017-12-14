package bad.robot.logging

import java.io.{ByteArrayOutputStream, OutputStream, PrintStream}

import org.slf4j.Logger

/**
  * Convert a [[org.slf4j.Logger]] to a [[java.io.PrintStream]].
  */
object ErrorLogger {

  def apply(log: Logger = Log): PrintStream = new PrintStream(new OutputStream {

    private val stream = new ByteArrayOutputStream(1000)

    def write(bytes: Int): Unit = {
      if (bytes == '\u0000') {
        val line = stream.toString
        stream.reset()
        log.error(line)
      } else {
        stream.write(bytes)
      }
    }
  })

}
