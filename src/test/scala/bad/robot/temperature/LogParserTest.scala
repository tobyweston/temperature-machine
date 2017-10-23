package bad.robot.temperature

import java.time.Instant

import org.specs2.mutable.Specification

class LogParserTest extends Specification {

  "Example inputs" >> {
    assert(
      "2017-10-20 10:40:05:976 [main] INFO Starting temperature-machine (server mode)...", 
      LogMessage(Instant.parse("2017-10-20T09:40:05.976Z"), "[main]", "INFO", "Starting temperature-machine (server mode)...")
    )
    assert(
      "2017-10-20 10:40:06:220 [main] INFO Ok", 
      LogMessage(Instant.parse("2017-10-20T09:40:06.220Z"), "[main]", "INFO", "Ok")
    )
    assert(
      "2017-10-20 10:40:12:743 [main] INFO Monitoring sensor file(s) on 'study'\n  /sys/bus/w1/devices/28-031591c760ff/w1_slave", 
      LogMessage(Instant.parse("2017-10-20T09:40:12.743Z"), "[main]", "INFO", "Monitoring sensor file(s) on 'study'\n  /sys/bus/w1/devices/28-031591c760ff/w1_slave")
    )
  }
  
  "Example log messages (not interested in the date etc)" >> {
    assertSuccess("2017-10-20 10:40:06:149 [main] INFO RRD initialising for 'study', 'bedroom1', 'bedroom2', 'bedroom3', 'outside', 'kitchen', 'lounge' (with up to 5 sensors each)...")
    assertSuccess("2017-10-20 10:40:12:328 [main] INFO Starting Discovery Server, listening for 'study', 'bedroom1', 'bedroom2', 'bedroom3', 'outside', 'kitchen', 'lounge'...")
    assertSuccess("2017-10-20 10:40:12:452 [temperature-machine-discovery-server-1] INFO Listening for broadcast messages...")
    assertSuccess("2017-10-20 10:40:12:743 [main] INFO Monitoring sensor file(s) on 'study'\n\t/sys/bus/w1/devices/28-031591c760ff/w1_slave")
    assertSuccess("2017-10-20 10:40:14:977 [main] INFO Temperature spikes greater than +/-25% will not be recorded")
    assertSuccess("2017-10-20 10:40:21:242 [main] INFO HTTP Server started on http://127.0.1.1:11900")
    assertSuccess("2017-10-20 10:57:14:560 [temperature-reading-thread-1] ERROR UnexpectedError(Failed to PUT temperature data to http://127.0.1.1:11900/temperature, response was 502 Bad Gateway: Error in RRD Bad sample time: 1508497034. Last update time was 1508497034, at least one second step is required)")
  }
  

  def assert(input: String, expected: LogMessage) = {
    LogParser.parseAll(LogParser.log, input) match {
      case LogParser.Success(result, _) => result must_== expected
      case error @ _                    => ko(error.toString)
    }
  }
  
  def assertSuccess(input: String) = {
    LogParser.parseAll(LogParser.log, input) match {
      case LogParser.Success(_, _) => ok
      case error @ _               => ko(error.toString)
    }
  }
}
