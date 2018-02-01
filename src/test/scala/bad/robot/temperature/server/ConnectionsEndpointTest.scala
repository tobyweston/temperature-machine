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
//import org.http4s.syntax.KleisliResponseOps
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification
import org.specs2.specification.AfterEach

class ConnectionsEndpointTest extends Specification with AfterEach {

  sequential

  "I fucking hate http4s" >> {
    val service: HttpService[IO] = ConnectionsEndpoint(fixedClock())

    val getRoot = Request[IO](Method.GET, Uri.uri("/"))
    // getRoot: org.http4s.Request[cats.effect.IO] = Request[IO](method=GET, uri=/, headers=Headers())

//    val io1: IO[Response[IO]] = service.orNotFound.run(getRoot)
    val io2: IO[Response[IO]] = http4sKleisliResponseSyntax(service).orNotFound.run(getRoot)
//    val io3: IO[Response[IO]] = new KleisliResponseOps(service).orNotFound.run(getRoot)

    val response = io2.unsafeRunSync
    // response: org.http4s.Response[cats.effect.IO] = Response(status=200, headers=Headers())

    response.status must_== Ok
  }

  "No recent connections" >> {
    val request = Request[IO](GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint(fixedClock())
    val response: IO[Response[IO]] =  service.orNotFound.run(request)
    response.flatMap(response => response.as[String]).unsafeRunSync() must_== "[]"
//    response.attemptAs[String].fold(throw _, identity).unsafeRunSync() must_== "[]"
//    response.as[String].unsafeRunSync() must_== "[]"
    response.unsafeRunSync().status must_== Ok
  }

  "After a connection is made" >> {
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("84.12.43.124")))

    val request = Request[IO](GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint(fixedClock())
    val response =  service.orNotFound.run(request)

    response.unsafeRunSync().status must_== Ok
    response.flatMap(_.as[String]).unsafeRunSync must_==
      """[
        |  {
        |    "host" : {
        |      "name" : "garage"
        |    },
        |    "ip" : {
        |      "value" : "84.12.43.124"
        |    }
        |  }
        |]""".stripMargin
  }

  "Recent connections show up" >> {
    val service = ConnectionsEndpoint(fixedClock(Instant.now.plus(4, minutes)))

    val request = Request[IO](GET, Uri.uri("/connections/active/within/5/mins"))
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("184.14.23.214")))
    val response =  service.orNotFound.run(request)

    response.unsafeRunSync().status must_== Ok
    response.flatMap(_.as[String]).unsafeRunSync must_== """[
                                                    |  {
                                                    |    "host" : {
                                                    |      "name" : "garage"
                                                    |    },
                                                    |    "ip" : {
                                                    |      "value" : "184.14.23.214"
                                                    |    }
                                                    |  }
                                                    |]""".stripMargin
  }

  "Multiple IPs" >> {
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("84.12.43.124", "10.0.1.12")))

    val request = Request[IO](GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint(fixedClock())
    val response =  service.orNotFound.run(request).unsafeRunSync()

    response.status must_== Ok
    response.as[String].unsafeRunSync must_==
      """[
        |  {
        |    "host" : {
        |      "name" : "garage"
        |    },
        |    "ip" : {
        |      "value" : "84.12.43.124, 10.0.1.12"
        |    }
        |  }
        |]""".stripMargin
  }

  "Connections expire / only recent connections show up" >> {
    val service = ConnectionsEndpoint(fixedClock(Instant.now.plus(6, minutes)))

    val request = Request[IO](GET, Uri.uri("/connections/active/within/5/mins"))
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("162.34.13.113")))
    val response =  service.orNotFound.run(request).unsafeRunSync()


    response.status must_== Ok
    response.as[String].unsafeRunSync must_== "[]"
  }

  def fixedClock(instant: Instant = Instant.now) = Clock.fixed(instant, ZoneId.systemDefault())

  def xForwardedFor(ipAddresses: String*) = {
    val ips = ipAddresses.map(ip => Some(InetAddress.getByName(ip)))
    `X-Forwarded-For`(NonEmptyList(ips.head, ips.tail.toList))
  }

  def after = ConnectionsEndpoint.reset()

}