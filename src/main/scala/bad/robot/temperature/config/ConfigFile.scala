package bad.robot.temperature.config

import java.io.PrintWriter

import bad.robot.logging
import bad.robot.logging.info
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{ConfigurationError, _}
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import knobs._

object ConfigFile {

  import scala.concurrent.ExecutionContext.Implicits.global     // todo replace with explicit one

  private val file = Files.path / "temperature-machine.cfg"

  private def exists = file.exists() && file.length() != 0

  
  def initWithUserInput: Stream[IO, Boolean] = {
    def ask(question: String, options: List[String]): IO[String] = {
      for {
        _     <- IO(print(s"$question ${options.mkString("[", "/", "]: ")}"))
        input <- IO(scala.io.StdIn.readLine())
        valid <- if (options.contains(input)) IO(input) else ask(question, options)
      } yield valid
    }
    
    def init(config: ConfigFile): IO[Boolean] = {
      for {
        exists <- IO(ConfigFile.exists)
        _      <- info(s"Creating config file at ${file.getAbsoluteFile}...").unlessA(exists)
        _      <- store(config).unlessA(exists)
        _      <- info(s"Config ${file.getAbsoluteFile} already exists, please edit it manually").whenA(exists)
      } yield !exists
    }
    
    Stream.eval(for {
      mode    <- ask("Do you want to run the temperature-machine as a server or client?", List("server", "client"))
      created <- init(BootstrapConfig.configFor(mode))
    } yield created)
  }

  def loadOrWarn(resource: KnobsResource = Required(FileResource.unwatched(file))): IO[Either[ConfigurationError, ConfigFile]] = {
    for {
      exists  <- IO(ConfigFile.exists)
      _       <- info(s"""The config file ${file.getAbsoluteFile} doesn't exist, run "temperature-machine --init" to create it.""").unlessA(exists)
      config  <- load(resource)
    } yield config
  }
  
  private def load(resource: KnobsResource): IO[Either[ConfigurationError, ConfigFile]] = {
    knobs.loadImmutable[IO](List(resource)).attempt.map(_.leftMap(error => {
      ConfigurationError(s"There was an error loading config; ${error.getMessage}")
    }).map(readConfigFile))
  }

  // todo maybe use 'lookup' rather than 'require' to avoid an exception being thrown?
  private val readConfigFile = (config: Config) => new ConfigFile {
    def mode: String = config.require[String]("mode")
    def hosts: List[String] = config.require[List[String]]("hosts")
  }

  private def store(config: ConfigFile): IO[Either[ConfigurationError, Boolean]] = {
    def write: Either[ConfigurationError, Boolean] = {
      if (!ConfigFile.exists) {
        val writer = new PrintWriter(file)
        val written = Either.catchNonFatal(writer.write(Template(config)))
          .leftMap(error => ConfigurationError(s"Failed to create new config file ${file.getAbsolutePath}; ${error.getMessage}"))
          .map(_ => true)
        writer.close()
        written
      } else {
        Right(false)
      }
    }

    for {
      written <- IO(write) 
      _       <- written.fold(cause => logging.error(cause.details), fileWritten => {
                   if (fileWritten) info("New config file created ok")
                   else info("Config file already exists")
                 })
    } yield written
  }

}

trait ConfigFile {
  def mode: String
  def hosts: List[String]
}

object Template {
  def apply(config: ConfigFile): String =
    s"""|
        |# valid values "client" or "server" only
        |mode = "${config.mode}"
        |hosts = ${config.hosts.mkString("[\"", "\", \"", "\"]")}
        |
        |""".stripMargin
}

private object BootstrapConfig {
  def configFor(mode: String) = mode match {
    case "client" => ClientBootstrapConfig()
    case _        => ServerBootstrapConfig()
  }

  case object ServerBootstrapConfig {
    def apply(): ConfigFile = new ConfigFile {
      def mode: String = "server"
      def hosts: List[String] = Set(Host.local.name, "study", "bedroom1", "bedroom2", "bedroom3", "outside", "kitchen", "lounge").toList
    }
  }

  case object ClientBootstrapConfig {
    def apply(): ConfigFile = new ConfigFile {
      def mode: String = "client"
      def hosts: List[String] = Nil
    }
  }
}