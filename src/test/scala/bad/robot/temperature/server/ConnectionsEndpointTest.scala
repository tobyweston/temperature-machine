package bad.robot.temperature.server

import java.net.InetAddress
import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.{Clock, Instant, ZoneId}

import bad.robot.temperature.rrd.Host
import cats.data.NonEmptyList
import cats.effect._
import org.http4s._
//import org.http4s.dsl.io._ - don't include this and org.http4s - they clash and cause ambiguity
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.implicits._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification

class ConnectionsEndpointTest extends Specification {

  sequential

  "No recent connections" >> {
    val request = Request[IO](GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint(Connections())(fixedClock())
    val response: IO[Response[IO]] =  service.orNotFound.run(request)
    response.flatMap(response => response.as[String]).unsafeRunSync() must_== "[]"
    response.unsafeRunSync().status must_== Ok
  }

  "After a connection is made" >> {
    val connections = Connections()
    connections.update(Host("garage"), Some(xForwardedFor("84.12.43.124")))

    val request = Request[IO](GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint(connections)(fixedClock())
    val response =  service.orNotFound.run(request)

    response.unsafeRunSync().status must_== Ok
    response.flatMap(_.as[String]).unsafeRunSync must_==
      """[
        |  {
        |    "host" : {
        |      "name" : "garage",
        |      "utcOffset" : null,
        |      "timezone" : null
        |    },
        |    "ip" : {
        |      "value" : "84.12.43.124"
        |    }
        |  }
        |]""".stripMargin
  }

  "Recent connections show up" >> {
    val connections = Connections()
    val service = ConnectionsEndpoint(connections)(fixedClock(Instant.now.plus(4, minutes)))

    val request = Request[IO](GET, Uri.uri("/connections/active/within/5/mins"))
    connections.update(Host("garage"), Some(xForwardedFor("184.14.23.214")))
    val response =  service.orNotFound.run(request)

    response.unsafeRunSync().status must_== Ok
    response.flatMap(_.as[String]).unsafeRunSync must_== """[
                                                    |  {
                                                    |    "host" : {
                                                    |      "name" : "garage",
                                                    |      "utcOffset" : null,
                                                    |      "timezone" : null
                                                    |    },
                                                    |    "ip" : {
                                                    |      "value" : "184.14.23.214"
                                                    |    }
                                                    |  }
                                                    |]""".stripMargin
  }

  "Multiple IPs" >> {
    val connections = Connections()
    connections.update(Host("garage"), Some(xForwardedFor("84.12.43.124", "10.0.1.12")))

    val request = Request[IO](GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint(connections)(fixedClock())
    val response =  service.orNotFound.run(request).unsafeRunSync()

    response.status must_== Ok
    response.as[String].unsafeRunSync must_==
      """[
        |  {
        |    "host" : {
        |      "name" : "garage",
        |      "utcOffset" : null,
        |      "timezone" : null
        |    },
        |    "ip" : {
        |      "value" : "84.12.43.124, 10.0.1.12"
        |    }
        |  }
        |]""".stripMargin
  }

  "Connections expire / only recent connections show up" >> {
    val connections = Connections()
    val service = ConnectionsEndpoint(connections)(fixedClock(Instant.now.plus(6, minutes)))

    val request = Request[IO](GET, Uri.uri("/connections/active/within/5/mins"))
    connections.update(Host("garage"), Some(xForwardedFor("162.34.13.113")))
    val response =  service.orNotFound.run(request).unsafeRunSync()


    response.status must_== Ok
    response.as[String].unsafeRunSync must_== "[]"
  }
  
  "Responses include X-Forwarded-Host header" >> {
    
    "Getting connections" >> {
      val request = Request[IO](GET, Uri.uri("/connections"))
      val service = ConnectionsEndpoint(Connections())(fixedClock())
      val response: Response[IO] =  service.orNotFound.run(request).unsafeRunSync()
      response.headers.toList.exists(_.name == "X-Forwarded-Host".ci) must_== true
    }

    "Getting recent connections" >> {
      val request = Request[IO](GET, Uri.uri("/connections/active/within/5/mins"))
      val service = ConnectionsEndpoint(Connections())(fixedClock())
      val response: Response[IO] =  service.orNotFound.run(request).unsafeRunSync()
      response.headers.toList.exists(_.name == "X-Forwarded-Host".ci) must_== true
    }

  }

  def fixedClock(instant: Instant = Instant.now) = Clock.fixed(instant, ZoneId.systemDefault())

  def xForwardedFor(ipAddresses: String*) = {
    val ips = ipAddresses.map(ip => Some(InetAddress.getByName(ip)))
    `X-Forwarded-For`(NonEmptyList(ips.head, ips.tail.toList))
  }

}