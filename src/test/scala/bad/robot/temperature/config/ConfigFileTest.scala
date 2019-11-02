package bad.robot.temperature.config

import java.io.File

import bad.robot.temperature.ConfigurationError
import bad.robot.temperature.config.ConfigFile.loadOrWarn
import org.specs2.mutable.Specification
import pureconfig.ConfigObjectSource
import pureconfig.backend.ConfigFactoryWrapper._

class ConfigFileTest extends Specification {

  "Can be loaded" >> {
    loadOrWarn(ConfigObjectSource(parseResources("example.cfg"))).unsafeRunSync() must beRight.like {
      case config => {
        config.mode must_== "server"
        config.hosts must_== List("one", "two")
    }
    }
  }
  
  "Retrieve no list of hosts" >> {
    val config = loadOrWarn(ConfigObjectSource(parseResources("example-empty.cfg"))).unsafeRunSync()
    config must beLeft.like { case error => {
      error must_== ConfigurationError("There was an error loading config; Key not found: 'hosts'.")
    }}
  }
  
  "Retrieve no mode" >> {
    val config = loadOrWarn(ConfigObjectSource(parseResources("bad-example-missing-fields.cfg"))).unsafeRunSync()
    config must beLeft.like { case error => {
      error must_== ConfigurationError("There was an error loading config; Key not found: 'mode'.")
    }}
  }

  "Retrieve the values configuration items" >> {
    loadOrWarn(ConfigObjectSource(parseResources("example.cfg"))).unsafeRunSync() must beRight.like { case config => {
      config.mode must_== "server"
      config.hosts must_== List("one", "two")
    }}
  }
  
  "Example failure messages" >> {
    loadOrWarn(ConfigObjectSource(parseFile(new File("/foo.cfg")))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("/foo.cfg (No such file or directory)")
    }
    loadOrWarn(ConfigObjectSource(parseFile(new File(sys.props("user.home"))))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain(s"${sys.props("user.home")} (Is a directory)")
    }
    loadOrWarn(ConfigObjectSource(parseResources("not/present/temperature-machine.cfg"))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("Unable to read resource not/present/temperature-machine.cfg (resource not found on classpath: not/present/temperature-machine.cfg")
    }
    loadOrWarn(ConfigObjectSource(parseResources("bad-example.cfg"))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("Unable to parse the configuration")
    }
    loadOrWarn(ConfigObjectSource(parseResources("bad-example-missing-values.cfg"))).unsafeRunSync() must beLeft.like {
      case error: ConfigurationError => error.details must contain("Unable to parse the configuration: Expecting end of input or a comma, got '=' (if you intended '=' to be part of a key or string value, try enclosing the key or value in double quotes")
    }
  }
  
}
