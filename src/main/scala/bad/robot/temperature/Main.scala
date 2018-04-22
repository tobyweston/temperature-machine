package bad.robot.temperature

import bad.robot.logging.error
import bad.robot.temperature.client.Client
import bad.robot.temperature.config.ConfigFile
import bad.robot.temperature.server.Server
import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}

object Main extends StreamApp[IO] {

  private val printErrorAndExit = (cause: ConfigurationError) => Stream.eval(error(cause.message)).flatMap(_ => Stream.emit(ExitCode(1)))

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, ExitCode] = {
    Stream
      .eval(ConfigFile.loadOrCreate())
      .flatMap(_.fold(printErrorAndExit, start(_)(requestShutdown)))
  }
  
  def start(config: ConfigFile): IO[Unit] => Stream[IO, ExitCode] = {
    config.mode match {
      case "server" => Server.stream(config.hosts, _)
      case "client" => Client.stream(Nil, _)
//      case _ => ???   
// todo error case?
    }
  }
  
}
