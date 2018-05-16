package bad.robot.logging

import java.util.logging.Level._
import java.util.logging._

import bad.robot.temperature.Files._

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
  setHandler(Logger.getLogger("sun.management.jmxremote"), CONFIG, createConsoleHandler)
  setHandler(Logger.getLogger("sun.management.jmxremote"), CONFIG, createFileHandler)

  private def createFileHandler: Handler = {
    val file = new FileHandler(LogFile.file)
    file.setLevel(CONFIG)
    file.setFormatter(new SimpleFormatter)
    file
  }
  
  private def createConsoleHandler: Handler = {
    val consoleHandler = new ConsoleHandler
    consoleHandler.setLevel(CONFIG)
    consoleHandler.setFormatter(new SimpleFormatter)
    consoleHandler
  }

  private def setHandler(logger: Logger, level: Level, consoleHandler: Handler) = {
    logger.setLevel(level)
    logger.addHandler(consoleHandler)
  }
}
