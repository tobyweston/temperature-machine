package bad.robot.temperature

import java.time.Instant

import org.specs2.mutable.Specification

class LogParserTest extends Specification {

  "Example dates" >> {
    assert(LogParser.instant, "2018-02-07 18:38:52:319+0100")
    assert(LogParser.instant, "2018-02-07 17:43:58:872+0000")
  }
  
  "Example inputs" >> {
    assert(
      "2017-10-31 10:40:05:976+0000 [main] INFO Starting temperature-machine (server mode)...", 
      LogMessage(Instant.parse("2017-10-31T10:40:05.976Z"), "[main]", "INFO", "Starting temperature-machine (server mode)...")
    )
    assert(
      "2017-10-31 10:40:06:220+0000 [main] INFO Ok", 
      LogMessage(Instant.parse("2017-10-31T10:40:06.220Z"), "[main]", "INFO", "Ok")
    )
    assert(
      "2017-10-31 10:40:12:743+0000 [main] INFO Monitoring sensor file(s) on 'study'\n  /sys/bus/w1/devices/28-031591c760ff/w1_slave", 
      LogMessage(Instant.parse("2017-10-31T10:40:12.743Z"), "[main]", "INFO", "Monitoring sensor file(s) on 'study'\n  /sys/bus/w1/devices/28-031591c760ff/w1_slave")
    )
    assert(
      "2018-05-15 20:44:05:358+0100 [sun.management.jmxremote] CONFIG JMX Connector ready at: service:jmx:rmi:///jndi/rmi://10.0.1.20:64493/jmxrmi",
      LogMessage(Instant.parse("2018-05-15T19:44:05.358Z"), "[sun.management.jmxremote]", "CONFIG", "JMX Connector ready at: service:jmx:rmi:///jndi/rmi://10.0.1.20:64493/jmxrmi")
    )
  }
  
  "Example log messages (not interested in the date etc)" >> {
    assert(LogParser.log, "2017-10-31 10:40:06:149+0000 [main] INFO RRD initialising for 'study', 'bedroom1', 'bedroom2', 'bedroom3', 'outside', 'kitchen', 'lounge' (with up to 5 sensors each)...")
    assert(LogParser.log, "2017-10-31 10:40:12:328+0000 [main] INFO Starting Discovery Server, listening for 'study', 'bedroom1', 'bedroom2', 'bedroom3', 'outside', 'kitchen', 'lounge'...")
    assert(LogParser.log, "2017-10-31 10:40:12:452+0000 [temperature-machine-discovery-server-1] INFO Listening for broadcast messages...")
    assert(LogParser.log, "2017-10-31 10:40:12:743+0000 [main] INFO Monitoring sensor file(s) on 'study'\n\t/sys/bus/w1/devices/28-031591c760ff/w1_slave")
    assert(LogParser.log, "2017-10-31 10:40:14:977+0000 [main] INFO Temperature spikes greater than +/-25% will not be recorded")
    assert(LogParser.log, "2017-10-31 10:40:21:242+0000 [main] INFO HTTP Server started on http://127.0.1.1:11900")
    assert(LogParser.log, "2017-10-31 10:57:14:560+0000 [temperature-reading-thread-1] ERROR UnexpectedError(Failed to PUT temperature data to http://127.0.1.1:11900/temperature, response was 502 Bad Gateway: Error in RRD Bad sample time: 1508497034. Last update time was 1508497034, at least one second step is required)")
  }
  
  "Minimal example that would cause stack overflow (against (.|\\s)*" >> {
    assert(
      LogParser.log, 
      """2017-11-01 19:33:48:270+0000 [blaze-nio-fixed-selector-pool-0] ERROR error in requestLoop()
        |java.lang.NullPointerException
        |        at org.http4s.internal.parboiled2.ParserInput$StringBasedParserInput.length(ParserInput.scala:97)
        |        at org.http4s.internal.parboiled2.Parser.__advance(Parser.scala:228)
        |        at org.http4s.internal.parboiled2.Parser.runRule$1(Parser.scala:140)
        |        at org.http4s.internal.parboiled2.Parser.phase0_initialRun$1(Parser.scala:150)
        |        at org.http4s.internal.parboiled2.Parser.__run(Parser.scala:203)
        |        at org.http4s.parser.Rfc2616BasicRules$.token(Rfc2616BasicRules.scala:84)
        |        at org.http4s.Method$.$anonfun$fromString$1(Method.scala:47)
        |        at scala.collection.MapLike.getOrElse(MapLike.scala:128)
        |        at scala.collection.MapLike.getOrElse$(MapLike.scala:126)
        |        at scala.collection.concurrent.TrieMap.getOrElse(TrieMap.scala:631)
        |        at org.http4s.Method$.fromString(Method.scala:47)
        |        at org.http4s.server.blaze.Http1ServerParser.collectMessage(Http1ServerParser.scala:48)
        |        at org.http4s.server.blaze.Http1ServerStage.runRequest(Http1ServerStage.scala:111)
        |        at org.http4s.server.blaze.Http1ServerStage.reqLoopCallback(Http1ServerStage.scala:97)
        |        at org.http4s.server.blaze.Http1ServerStage.$anonfun$requestLoop$1(Http1ServerStage.scala:74)
        |        at org.http4s.server.blaze.Http1ServerStage.$anonfun$requestLoop$1$adapted(Http1ServerStage.scala:74)
      """.stripMargin
    )
  }
  
  "Larger example that would cause stack overflow (against (.|\\s)*" >> {
    assert(
      LogParser.log, 
      """2017-11-01 19:33:48:270+0000 [blaze-nio-fixed-selector-pool-0] ERROR error in requestLoop()
        |java.lang.NullPointerException
        |        at org.http4s.internal.parboiled2.ParserInput$StringBasedParserInput.length(ParserInput.scala:97)
        |        at org.http4s.internal.parboiled2.Parser.__advance(Parser.scala:228)
        |        at org.http4s.internal.parboiled2.Parser.runRule$1(Parser.scala:140)
        |        at org.http4s.internal.parboiled2.Parser.phase0_initialRun$1(Parser.scala:150)
        |        at org.http4s.internal.parboiled2.Parser.__run(Parser.scala:203)
        |        at org.http4s.parser.Rfc2616BasicRules$.token(Rfc2616BasicRules.scala:84)
        |        at org.http4s.Method$.$anonfun$fromString$1(Method.scala:47)
        |        at scala.collection.MapLike.getOrElse(MapLike.scala:128)
        |        at scala.collection.MapLike.getOrElse$(MapLike.scala:126)
        |        at scala.collection.concurrent.TrieMap.getOrElse(TrieMap.scala:631)
        |        at org.http4s.Method$.fromString(Method.scala:47)
        |        at org.http4s.server.blaze.Http1ServerParser.collectMessage(Http1ServerParser.scala:48)
        |        at org.http4s.server.blaze.Http1ServerStage.runRequest(Http1ServerStage.scala:111)
        |        at org.http4s.server.blaze.Http1ServerStage.reqLoopCallback(Http1ServerStage.scala:97)
        |        at org.http4s.server.blaze.Http1ServerStage.$anonfun$requestLoop$1(Http1ServerStage.scala:74)
        |        at org.http4s.server.blaze.Http1ServerStage.$anonfun$requestLoop$1$adapted(Http1ServerStage.scala:74)
        |        at scala.concurrent.impl.CallbackRunnable.run(Promise.scala:60)
        |        at org.http4s.blaze.util.Execution$$anon$1.execute(Executor.scala:27)
        |        at scala.concurrent.impl.CallbackRunnable.executeWithValue(Promise.scala:68)
        |        at scala.concurrent.impl.Promise$DefaultPromise.$anonfun$tryComplete$1(Promise.scala:284)
        |        at scala.concurrent.impl.Promise$DefaultPromise.$anonfun$tryComplete$1$adapted(Promise.scala:284)
        |        at scala.concurrent.impl.Promise$DefaultPromise.tryComplete(Promise.scala:284)
        |        at org.http4s.blaze.channel.nio1.NIO1HeadStage.readReady(NIO1HeadStage.scala:61)
        |        at org.http4s.blaze.channel.nio1.SelectorLoop.run(SelectorLoop.scala:134)
        |""".stripMargin
    )
  }

  "Error on spike message parses" >> {
    assert(
      LogParser.log,
      """2017-11-13 21:53:33:010+0000 [temperature-http-server-1] WARN An unexpected spike was encountered on:
         | sensor(s)             : 28-0115735605ff
         | previous temperatures : 20.1 °C
         | spiked temperatures   : 29.4 °C
         |""".stripMargin
    ) 
  }

  def assert(input: String, expected: LogMessage) = {
    LogParser.parseAll(LogParser.log, input) match {
      case LogParser.Success(result, _) => result must_== expected
      case error @ _                    => ko(error.toString)
    }
  }
  
  def assert(parser: LogParser.Parser[_], input: String) = {
    LogParser.parseAll(parser, input) match {
      case LogParser.Success(_, _) => ok
      case error @ _               => ko(error.toString)
    }
  }
}
