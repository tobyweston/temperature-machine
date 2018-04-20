package bad.robot.temperature.config

import java.io.{File, PrintWriter}

import bad.robot.logging
import bad.robot.logging.info
import bad.robot.temperature.{ConfigurationError, _}
import bad.robot.temperature.rrd.Host
import cats.effect.IO
import cats.implicits._
import knobs._

object ConfigFile {

  import scala.concurrent.ExecutionContext.Implicits.global     // todo replace with explicit one

  private val path = new File(sys.props("user.home")) / ".temperature" / "temperature-machine.cfg"

  private def fileExists = path.exists() && path.length() != 0

  def init(): IO[Boolean] = {
    for {
      exists <- IO(fileExists)
      _      <- info(s"Creating config file at ${path.getAbsoluteFile}...").unlessA(exists)
      _      <- store(BootstrapConfig()).unlessA(exists)
      _      <- info(s"Using config ${path.getAbsoluteFile}").whenA(exists)
    } yield !exists
  }

  def loadOrCreate(resource: KnobsResource = Required(FileResource.unwatched(path))): IO[Either[ConfigurationError, ConfigFile]] = {
    for {
      _       <- init()
      config  <- load(resource)
    } yield config
  }
  
  def load(resource: KnobsResource = Required(FileResource.unwatched(path))): IO[Either[ConfigurationError, ConfigFile]] = {
    knobs.loadImmutable[IO](List(resource)).attempt.map(_.leftMap(error => {
      ConfigurationError(s"There was an error loading config from ${path.getAbsolutePath}; ${error.getMessage}")
    }).map(readConfigFile))
  }

  // todo maybe use 'lookup' rather than 'require' to avoid a thrown exception?
  private val readConfigFile = (config: Config) => new ConfigFile {
    def mode: String = config.require[String]("mode")
    def hosts: List[String] = config.require[List[String]]("hosts")
  }

  private def store(config: ConfigFile): IO[Either[ConfigurationError, Boolean]] = {
    
    def write: Either[ConfigurationError, Boolean] = {
      if (!fileExists) {
        val writer = new PrintWriter(path)
        val written = Either.catchNonFatal(writer.write(Template(config)))
          .leftMap(error => ConfigurationError(s"Failed to create new config file ${path.getAbsolutePath}; ${error.getMessage}"))
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
        |mode = "${config.mode}"
        |hosts = ${config.hosts.mkString("[\"", "\", \"", "\"]")}
        |
        |""".stripMargin
}

case object BootstrapConfig {
  def apply(): ConfigFile = new ConfigFile {
    def mode: String = "server"
    def hosts: List[String] = List(Host.local.name, "study", "bedroom1", "bedroom2", "bedroom3", "outside", "kitchen", "lounge")
  }
}