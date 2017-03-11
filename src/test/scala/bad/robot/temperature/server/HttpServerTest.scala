package bad.robot.temperature.server

import java.io.{BufferedWriter, File, FileWriter}

import bad.robot.temperature.rrd.{Host, RrdFile}
import org.http4s.Method.GET
import org.http4s.client.blaze._
import org.http4s.{Request, Status, Uri}
import org.specs2.mutable.Specification

import scalaz.concurrent.Task

class HttpServerTest extends Specification {

  "When the Http server has been started" >> {
    val server = HttpServer(8080, List(Host("example"))).unsafePerformSync
    val client = SimpleHttp1Client()

    "index.html can be loaded" >> {
      assertOk(Request(GET, path("")))
    }

    "temperature.json can be loaded" >> {
      createFile("temperature.json")
      assertOk(Request(GET, path("/temperature.json")))
    }

    "RRD chart / image can be loaded" >> {
      createFile("temperature-1-days.png")
      assertOk(Request(GET, path("/temperature-1-days.png")))
    }

    "Some java script can be loaded (note this changes with every UI deployment)" >> {
      assertOk(Request(GET, path("/static/js/main.1a5d93b1.js")))
    }

    "Some css can be loaded (note this changes with every UI deployment)" >> {
      assertOk(Request(GET, path("/static/css/main.0724c9df.css")))
    }

    "image can be loaded" >> {
      assertOk(Request(GET, path("/images/spinner.gif")))
    }

    "media can be loaded" >> {
      assertOk(Request(GET, path("/static/media/glyphicons-halflings-regular.f4769f9b.eot")))
    }

    def assertOk(request: Request) = {
      val response = client.fetch(request)(Task.delay(_)).unsafePerformSync
      response.status must_== Status.Ok
    }

    def path(url: String): Uri = Uri.fromString(s"http://localhost:8080$url").getOrElse(throw new Exception(s"bad url $url"))

    def createFile(filename: String) = {
      val file = new File(s"${RrdFile.path}/$filename")
      println(file.getAbsoluteFile)
      val writer = new BufferedWriter(new FileWriter(file))
      writer.close()
    }

    step {
      server.shutdown()
    }
  }

}
