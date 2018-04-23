package bad.robot.temperature

import bad.robot.logging._
import bad.robot.temperature.CommandLineHelp._
import bad.robot.temperature.client.Client
import bad.robot.temperature.config.ConfigFile
import bad.robot.temperature.server.Server
import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}

object Main extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, ExitCode] = {
    args match {
      case option :: Nil if List("-v", "--version").contains(option) => printVersion
      case option :: Nil if List("-h", "--help").contains(option)    => printUsage
      case Nil                                                       => startupBasedOnConfigFile(requestShutdown)
      case options                                                   => Stream
                                                                          .eval(IO(print(s"Invalid option: ${options.mkString(" ")}")))
                                                                          .flatMap(_ => printUsage)
    }
  }

  private def startupBasedOnConfigFile(requestShutdown: IO[Unit]) = {
    def start(config: ConfigFile): IO[Unit] => Stream[IO, ExitCode] = {
      config.mode match {
        case "client" => Client.stream(Nil, _)
        case "server" => Server.stream(config.hosts, _)
        case mode     => _ => printConfigError(mode)
      }
    }

    Stream
      .eval(ConfigFile.loadOrCreate())
      .flatMap(_.fold(printErrorAndExit, start(_)(requestShutdown)))
  }
}

object CommandLineHelp {

  val printErrorAndExit = (cause: ConfigurationError) => exitAfter(Stream.eval(error(cause.message)))

  def printVersion = exitAfter(Stream.eval(IO(println(s"${BuildInfo.name} ${BuildInfo.version} (${BuildInfo.latestSha})"))))

  def printUsage = exitAfter(Stream.eval(IO(println(
    """
      |Usage: temperature-machine [options]
      |
      |-v, --version        display the current version
      |-h, --help           this page, try 'man temperature-machine' or http://temperature-machine.com for more help
      |-i, --init           create the default config file. Once the config file exists, modify it as 
      |                     required then run again with no options to start the temperature machine
      |
      |Run with no options to start the temperature-machine
      |
    """.stripMargin))))

  def printConfigError(mode: String) = exitAfter(Stream.eval(error(
    s"""Error in configuration file, 'mode' was set to "$mode" but only "client" or "server" are allowed"""
  )))

  def exitAfter(io: Stream[IO, _]) = io.flatMap(_ => Stream.emit(ExitCode(1)))
}
