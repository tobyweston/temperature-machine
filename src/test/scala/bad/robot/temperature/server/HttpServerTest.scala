package bad.robot.temperature.server

import java.io.{BufferedWriter, File, FileWriter}

import bad.robot.logging._
import bad.robot.temperature.rrd.{Host, RrdFile}
import org.http4s.Method.GET
import org.http4s.client.blaze.BlazeClientConfig._
import org.http4s.client.blaze._
import org.http4s.{Request, Status, Uri}
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scalaz.concurrent.Task

class HttpServerTest extends Specification {

  "When the Http server has been started" >> {
    val server = HttpServer(8080, List(Host("example"))).unsafePerformSync
    val client = PooledHttp1Client(config = defaultConfig.copy(idleTimeout = 30 minutes, responseHeaderTimeout = 30 minutes))

    // todo wait for server to startup, not sure how.
    
    "index.html can be loaded" >> {
      assertOk(Request(GET, path("")))
    }

    "temperature.json can be loaded" >> {
      createJsonFile()
      assertOk(Request(GET, path("/temperature.json")))
    }

    "temperature.csv can be loaded" >> {
      createJsonFile()
      assertOk(Request(GET, path("/temperatures.csv")))
    }

    "RRD chart / image can be loaded" >> {
      createFile("temperature-1-days.png")
      assertOk(Request(GET, path("/temperature-1-days.png")))
    }

    "Some java script can be loaded (note this changes with every UI deployment)" >> {
      assertOk(Request(GET, path("/static/js/main.ad1feb78.js")))
    }

    "Some css can be loaded (note this changes with every UI deployment)" >> {
      assertOk(Request(GET, path("/static/css/main.b58db282.css")))
    }

    "image can be loaded" >> {
      assertOk(Request(GET, path("/images/spinner.gif")))
    }

    "media can be loaded" >> {
      assertOk(Request(GET, path("/static/media/glyphicons-halflings-regular.f4769f9b.eot")))
    }

    "list of connections can be retrieved" >> {
      assertOk(Request(GET, path("/connections")))
    }

    "list of recent connections can be retrieved" >> {
      assertOk(Request(GET, path("/connections/active/within/5/mins")))
    }

    "get the local machines log over http" >> {
      assertOk(Request(GET, path("/log")))
    }
    
    def assertOk(request: Request) = {
      val response = client.fetch(request)(Task.delay(_)).unsafePerformSync
      response.status must be_==(Status.Ok).eventually(10, 500 milliseconds)
    }

    def path(url: String): Uri = Uri.fromString(s"http://localhost:8080$url").getOrElse(throw new Exception(s"bad url $url"))

    def createJsonFile() = {
      val exampleJson =
        """
          |[
          |  {
          |    "label": "bedroom1-sensor-1",
          |    "data": [
          |      {
          |        "x": 1507709610000,
          |        "y": "NaN"
          |      },
          |      {
          |        "x": 1507709640000,
          |        "y": "+2.2062500000E01"
          |      },
          |      {
          |        "x": 1507709680000,
          |        "y": "+2.2262500000E01"
          |      }
          |    ]
          |  }
          |]
        """.stripMargin

      val writer = new BufferedWriter(new FileWriter(JsonFile.file))
      writer.write(exampleJson)
      writer.close()
    }
    
    def createFile(filename: String) = {
      val file = new File(s"${RrdFile.path}/$filename")
      val writer = new BufferedWriter(new FileWriter(file))
      writer.close()
    }

    step {
      val shutdown = for {
        _ <- server.shutdown()
        _ <- Task.delay(Log.info(s"HTTP Server shutting down"))
      } yield ()
      shutdown.unsafePerformSync
    }
  }

}
