package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature._
import org.http4s.Method._
import org.http4s.Status.ResponseClass.Successful
import org.http4s.Uri.{Authority, IPv4}
import org.http4s.util.string._
import org.http4s.{Headers, Request, Response, Uri}
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.util.NonEmptyList

import scalaz.concurrent.Task
import scalaz.{-\/, \/, \/-}

case class HttpUpload(address: InetAddress) extends TemperatureWriter {

  private val blaze = BlazeHttpClient()

  def write(measurement: Measurement): Error \/ Unit = {
    val request = Request(PUT, Uri(
      scheme = Some("http".ci),
      authority = Some(Authority(host = IPv4(address.getHostAddress), port = Some(11900))),
      path = "/temperature"),
      headers = Headers(`X-Forwarded-For`(currentIpAddress))
    ).withBody(encode(measurement).spaces2).unsafePerformSync

    blaze.fetch(request) {
      case Successful(_)   => Task.now(\/-(()))
      case Error(response) => Task.now(-\/(UnexpectedError(s"Failed to PUT temperature data to ${request.uri.renderString}, response was ${response.status}: ${response.as[String].unsafePerformSync}")))
    }.handleWith({
      case t: Throwable    => Task.now(-\/(UnexpectedError(s"Failed to connect to $address\n\nError was $t")))
    }).unsafePerformSync
  }

  private def currentIpAddress = NonEmptyList(Some(InetAddress.getLocalHost))
}

object Error {
  def unapply(response: Response): Option[Response] = if (response.status.code >= 300) Some(response) else None
}
