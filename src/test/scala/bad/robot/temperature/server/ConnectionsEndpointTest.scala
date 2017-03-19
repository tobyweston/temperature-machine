package bad.robot.temperature.server

import java.net.InetAddress

import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.dsl._
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.util.NonEmptyList
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification
import org.specs2.specification.AfterEach


class ConnectionsEndpointTest extends Specification with AfterEach {

  sequential

  "No recent connections" >> {
    val request = Request(GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint.service
    val response = service(request).unsafePerformSync
    response.as[String].unsafePerformSync must_== "[]"
    response.status must_== Ok
  }

  "After a connection is made" >> {
    ConnectionsEndpoint.update(Some(xForwardedFor("84.12.43.124")))

    val request = Request(GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint.service
    val response = service(request).unsafePerformSync

    response.status must_== Ok
    response.as[String].unsafePerformSync must_==
      """[
         |  "84.12.43.124"
         |]""".stripMargin
  }


  def xForwardedFor(ipAddress: String) = {
    `X-Forwarded-For`(NonEmptyList(Some(InetAddress.getByName(ipAddress))))
  }

  def after = ConnectionsEndpoint.reset()

  }