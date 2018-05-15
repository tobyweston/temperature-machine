package bad.robot.logging

import java.util.logging.{Level, Logger}
import java.util.logging.ConsoleHandler
import java.util.logging.SimpleFormatter

/**
  * Capture the following properties programmatically
  * 
  * {{{
  * handlers=java.util.logging.ConsoleHandler
  * java.util.logging.ConsoleHandler.level=CONFIG
  * java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
  * java.util.logging.SimpleFormatter.format=\u0000%1$tF %1$tH:%1$tM:%1$tS:%1$tL%1$tz [%3$s] %4$s %5$s %n
  * sun.management.jmxremote.level=CONFIG
  * }}}
  */
class JavaLoggingConfig {

  System.setProperty("java.util.logging.SimpleFormatter.format", "\u0000%1$tF %1$tH:%1$tM:%1$tS:%1$tL%1$tz [%3$s] %4$s %5$s %n")
  setHandler(Logger.getLogger("sun.management.jmxremote"), Level.CONFIG, createHandler)

  private def createHandler: ConsoleHandler = {
    val consoleHandler = new ConsoleHandler
    consoleHandler.setLevel(Level.CONFIG)
    consoleHandler.setFormatter(new SimpleFormatter)
    consoleHandler
  }

  private def setHandler(logger: Logger, level: Level, consoleHandler: ConsoleHandler) = {
    logger.setLevel(level)
    logger.addHandler(consoleHandler)
  }
}
