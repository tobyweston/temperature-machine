package bad.robot.temperature.client

import java.net.{InetAddress, NetworkInterface}

import bad.robot.temperature.IpAddress._
import bad.robot.temperature._
import cats.effect.{IO, Resource}
import org.http4s.Status.Successful
import org.http4s.Uri.{Authority, IPv4, Scheme}
import org.http4s.client.dsl.Http4sClientDsl.WithBodyOps
import org.http4s.client.{Client => Http4sClient}
import org.http4s.dsl.io._
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.{Uri, _}
import scalaz.{-\/, \/, \/-}

import scala.collection.JavaConverters._

// todo retain Client[IO] rather than Resource[...]
case class HttpUpload(address: InetAddress, client: Resource[IO, Http4sClient[IO]]) extends TemperatureWriter {

  private implicit val encoder = jsonEncoder[Measurement]

  private val decoder = EntityDecoder.text[IO]
  
  def write(measurement: Measurement): Error \/ Unit = {
    val uri = Uri(
      scheme = Some(Scheme.http),
      authority = Some(Authority(host = IPv4(address.getHostAddress), port = Some(11900))),
      path = "/temperature"
    )

    val request: IO[Request[IO]] = PUT.apply(measurement, uri, `X-Forwarded-For`(currentIpAddress))

    val fetch: IO[Error \/ Unit] = client.use { http => http.fetch(request) {
      case Successful(_) => IO.pure(\/-(()))
      case error @ _     => IO(-\/(UnexpectedError(s"Failed to PUT temperature data to ${uri.renderString}, response was ${error.status}: ${error.as[String](implicitly, decoder).attempt.unsafeRunSync}")))
    }}

    // why no leftMap?
    fetch.attempt.map {
      case Left(t)      => -\/(UnexpectedError(s"Failed attempting to connect to $address to send $measurement\n\nError was: $t"))
      case Right(value) => value
    }.unsafeRunSync()
  }
}

object HttpUpload {

  val allNetworkInterfaces: List[NetworkInterface] = {
    NetworkInterface.getNetworkInterfaces
      .asScala
      .toList
      .filter(_.isUp)
      .filterNot(_.isLoopback)
  }

}