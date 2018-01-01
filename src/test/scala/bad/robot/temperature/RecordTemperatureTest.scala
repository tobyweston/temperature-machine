package bad.robot.temperature

import bad.robot.temperature.TestAppender.Name
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.RecordTemperature
import org.apache.logging.log4j.Level._
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.{LogEvent, LoggerContext}
import org.apache.logging.log4j.{Level, LogManager}
import org.specs2.mutable.Specification

import scalaz.{-\/, \/-}

class RecordTemperatureTest extends Specification {
  sequential

  "Take a measurement and write it out" >> {
    val input = new TemperatureReader {
      def read = \/-(List(SensorReading("example", Temperature(69.9))))
    }
    val output = new TemperatureWriter {
      var temperatures = List[Temperature]()

      def write(measurement: Measurement) = {
        this.temperatures = measurement.temperatures.map(_.temperature)
        \/-(Unit)
      }
    }
    RecordTemperature(Host("example"), input, output, null).run()
    output.temperatures must_== List(Temperature(69.9))
  }

  "Fail to take a measurement" >> {
    val input = new TemperatureReader {
      def read = -\/(UnexpectedError("whatever"))
    }
    val output = new TemperatureWriter {
      def write(measurement: Measurement) = ???
    }

    val logger = LogManager.getRootLogger
    val appender = new TestAppender(logger.getName, ERROR)

    RecordTemperature(Host("example"), input, output, logger).run()

    appender.cleanup(logger.getName)
    
    appender.messages.head must_== s"UnexpectedError(whatever)"
  }

  "Take a measurement but fail to write it" >> {
    val input = new TemperatureReader {
      def read = \/-(List(SensorReading("example", Temperature(69.9))))
    }
    val output = new TemperatureWriter {
      def write(measurement: Measurement) = -\/(UnexpectedError("whatever trevor"))
    }

    val logger = LogManager.getRootLogger
    val appender = new TestAppender(logger.getName, ERROR)

    RecordTemperature(Host("example"), input, output, logger).run()

    appender.cleanup(logger.getName)
    
    appender.messages.head must_== s"UnexpectedError(whatever trevor)"
  }

}


object TestAppender {
  
  val Name = "TestAppender"
  
  def getAppender: TestAppender = {
    val context = LogManager.getContext.asInstanceOf[LoggerContext]
    context.getConfiguration.getAppender(Name).asInstanceOf[TestAppender]
  }
}

class TestAppender(loggerName: String, level: Level) extends AbstractAppender(Name, null, PatternLayout.createDefaultLayout()) {
  
  private val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
  
  var messages = List[String]()

  addToLogger(loggerName, level)
  
  def append(event: LogEvent): Unit = {
    messages = messages :+ event.getMessage.getFormattedMessage
  }

  private def addToLogger(loggerName: String, level: Level) = {
    val config = context.getConfiguration.getLoggerConfig(loggerName)
    config.addAppender(this, level, null)
    context.updateLoggers()
    this.start()
  }

  def cleanup(loggerName: String) = {
    val config = context.getConfiguration.getLoggerConfig(loggerName)
    config.removeAppender(Name)
    context.updateLoggers()
    this.stop()
  }

}