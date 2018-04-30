package bad.robot.temperature.config

import java.io.File

import bad.robot.temperature.ConfigurationError
import bad.robot.temperature.config.ConfigFile.loadOrWarn
import knobs._
import org.specs2.mutable.Specification

class ConfigFileTest extends Specification {

  "Can be loaded" >> {
    loadOrWarn(Required(ClassPathResource("example.cfg"))).unsafeRunSync() must beRight
  }
  
  "Retrieve empty list for hosts" >> {
    loadOrWarn(Required(ClassPathResource("example-empty.cfg"))).unsafeRunSync() must beRight.like { case config => {
      config.mode must_== "client"
      config.hosts must throwA[KeyError]
    }}
  }
  
  "Retrieve no mode" >> {
    val config = loadOrWarn(Required(ClassPathResource("bad-example-missing-fields.cfg"))).unsafeRunSync()
    config must beRight.like { case config => {
      config.mode must throwA[KeyError]
      config.hosts must_== List("one", "two")
    }}
  }

  "Retrieve the values configuration items" >> {
    loadOrWarn(Required(ClassPathResource("example.cfg"))).unsafeRunSync() must beRight.like { case config => {
      config.mode must_== "server"
      config.hosts must_== List("one", "two")
    }}
  }
  
  "Example failure messages" >> {
    import scala.concurrent.ExecutionContext.Implicits.global     // todo replace with explicit one

    loadOrWarn(Required(FileResource.unwatched(new File("/foo.cfg")))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("/foo.cfg (No such file or directory)")
    }
    loadOrWarn(Required(FileResource.unwatched(new File(sys.props("user.home"))))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain(s"${sys.props("user.home")} (Is a directory)")
    }
    loadOrWarn(Required(ClassPathResource("not/present/temperature-machine.cfg"))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("not/present/temperature-machine.cfg not found on classpath")
    }
    loadOrWarn(Required(ClassPathResource("bad-example.cfg"))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("expected configuration")
    }
    loadOrWarn(Required(ClassPathResource("bad-example-missing-values.cfg"))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("expected comment, newline, value, or whitespace")
    }
  }
  
}
