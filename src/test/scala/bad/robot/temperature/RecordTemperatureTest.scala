package bad.robot.temperature

import bad.robot.temperature.TestAppender.Name
import bad.robot.temperature.rrd.{Host, Seconds}
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
    val time = Seconds.now()
    val input = new TemperatureReader {
      def read = \/-(Measurement(Host("A"), time, List(SensorReading("example", Temperature(69.9)))))
    }
    val output = new TemperatureWriter {
      var measurements = List[Measurement]()

      def write(measurement: Measurement) = {
        this.measurements = measurement :: measurements
        \/-(Unit)
      }
    }
    RecordTemperature(input, output, null).run()
    output.measurements must_== List(Measurement(Host("A"), time, List(SensorReading("example", Temperature(69.9)))))
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

    RecordTemperature(input, output, logger).run()

    appender.cleanup(logger.getName)
    
    appender.messages.head must_== s"UnexpectedError(whatever)"
  }

  "Take a measurement but fail to write it" >> {
    val input = new TemperatureReader {
      def read = \/-(Measurement(Host("A"), Seconds.now(), List(SensorReading("example", Temperature(69.9)))))
    }
    val output = new TemperatureWriter {
      def write(measurement: Measurement) = -\/(UnexpectedError("whatever trevor"))
    }

    val logger = LogManager.getRootLogger
    val appender = new TestAppender(logger.getName, ERROR)

    RecordTemperature(input, output, logger).run()

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