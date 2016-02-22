package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature._
import org.http4s.Method._
import org.http4s.Status.ResponseClass.{Successful, ServerError}
import org.http4s.Uri.{Authority, IPv4}
import org.http4s.util.string._
import org.http4s.{Response, Request, Uri}

import scalaz.concurrent.Task
import scalaz.{-\/, \/, \/-}

case class HttpUpload(address: InetAddress) extends TemperatureWriter {

  private val blaze = BlazeHttpClient()

  def write(measurement: Measurement): Error \/ Unit = {
    val request = Request(PUT, Uri(
      scheme = Some("http".ci),
      authority = Some(Authority(host = IPv4(address.getHostAddress), port = Some(11900))),
      path = "/temperature")
    ).withBody(encode(measurement).spaces2).run
    blaze.apply(request).map({
      case Successful(_) => \/-(())
      case Error(response) => -\/(UnexpectedError(s"Failed to PUT temperature data to ${request.uri.renderString}, response was ${response.status}: ${response.as[String].run} "))
    }).handleWith({
      case t: Throwable => Task.now(-\/(UnexpectedError(s"Failed to connect to $address\n\nError was $t")))
    }).run
  }
}

object Error {
  def unapply(response: Response): Option[Response] = if (response.status.code >= 300) Some(response) else None
}
