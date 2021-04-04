package bad.robot.temperature

import bad.robot.logging._
import bad.robot.temperature.CommandLineHelp._
import bad.robot.temperature.client.Client
import bad.robot.temperature.config.ConfigFile
import bad.robot.temperature.server.Server
import cats.effect.{ExitCode, IO, IOApp, _}
import cats.implicits._

class Main[F[_]](implicit F: ConcurrentEffect[IO], timer: Timer[IO], cs: ContextShift[IO])  {

  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case option :: Nil if List("-v", "--version").contains(option) => printVersion
      case option :: Nil if List("-h", "--help").contains(option)    => printUsage
      case option :: Nil if List("-i", "--init").contains(option)    => exitWithErrorAfter(ConfigFile.initWithUserInput)
      case Nil                                                       => startupBasedOnConfigFile
      case options                                                   => IO(print(s"Invalid option: ${options.mkString(" ")}"))
                                                                          .flatMap(_ => printUsage)
    }
  }

  private def startupBasedOnConfigFile: IO[ExitCode] = {
    def start(config: ConfigFile): IO[ExitCode] = {
      config.mode match {
        case "client" => Client(Nil)
        case "server" => Server(config.hosts)
        case mode     => printConfigError(mode)
      }
    }

    ConfigFile.loadOrWarn().flatMap(_.fold(printErrorAndExit, start))
  }
}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    new Main[IO].run(args).as(ExitCode.Success)
  }
}

object CommandLineHelp {

  val printErrorAndExit = (cause: ConfigurationError) => exitWithErrorAfter(error(cause.message))

  def printVersion = exitWithErrorAfter(IO(println(s"${BuildInfo.name} ${BuildInfo.version} (${BuildInfo.latestSha})")))

  def printUsage = exitWithErrorAfter(IO(println(
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
    """.stripMargin)))

  def printConfigError(mode: String) = exitWithErrorAfter(error(
    s"""Error in configuration file, 'mode' was set to "$mode" but only "client" or "server" are allowed"""
  ))

  def exitWithErrorAfter(io: IO[_]): IO[ExitCode] = io.flatMap(_ => IO(ExitCode.Error))
}
