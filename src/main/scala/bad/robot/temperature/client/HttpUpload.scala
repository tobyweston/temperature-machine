package bad.robot.temperature.client

import java.net.{InetAddress, NetworkInterface}

import bad.robot.temperature.IpAddress._
import bad.robot.temperature.client.HttpUpload.currentIpAddress
import bad.robot.temperature.{JsonOps, _}
import cats.data.NonEmptyList
import cats.effect.IO
import org.http4s.Status.Successful
import org.http4s.Uri.{Authority, IPv4, Scheme}
import org.http4s.client.dsl.Http4sClientDsl.WithBodyOps
import org.http4s.client.{Client => Http4sClient}
import org.http4s.dsl.io._
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.{Uri, _}

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-}

case class HttpUpload(address: InetAddress, client: Http4sClient[IO]) extends TemperatureWriter {

  private implicit val encoder = jsonEncoder[Measurement]

  private val decoder = EntityDecoder.text[IO]
  
  def write(measurement: Measurement): Error \/ Unit = {
    val uri = Uri(
      scheme = Some(Scheme.http),
      authority = Some(Authority(host = IPv4(address.getHostAddress), port = Some(11900))),
      path = "/temperature"
    )

    val request: IO[Request[IO]] = PUT.apply(uri, measurement, `X-Forwarded-For`(currentIpAddress))

    val fetch: IO[Error \/ Unit] = client.fetch(request) {
      case Successful(_) => IO.pure(\/-(()))
      case error @ _     => IO(-\/(UnexpectedError(s"Failed to PUT temperature data to ${uri.renderString}, response was ${error.status}: ${error.as[String](implicitly, decoder).attempt.unsafeRunSync}")))
    }

    // why no leftMap?
    fetch.attempt.map {
      case Left(t)      => -\/(UnexpectedError(s"Failed attempting to connect to $address to send $measurement\n\nError was: $t\nPayload was: '${encode(measurement).spaces2ps}'\n"))
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

  val currentIpAddress: NonEmptyList[Option[InetAddress]] = {
    val addresses = for {
      interface <- allNetworkInterfaces
      address <- interface.getInetAddresses.asScala
      ip <- isIpAddress(address.getHostAddress).option(address)
    } yield Some(ip)

    NonEmptyList(addresses.head, addresses.tail)
  }
}