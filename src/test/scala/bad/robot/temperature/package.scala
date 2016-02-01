package bad.robot.temperature

import org.http4s.{Header, Response, Status}
import org.specs2.matcher.{MatchResult, Expectable, Matcher}

package object test {

  def haveStatus(status: Status) = new Matcher[Response] {
    def apply[S <: Response](e: Expectable[S]): MatchResult[S] = result(
      e.value.status == status,
      s"""Status of [${e.value.status}]
          |
         |is
          |
         |$status""".stripMargin,
      s"""Status of
          |[${e.value.status}]
          |
         |is not
          |
         |[$status]
          |
         |(${e.value.as[String].run})""".stripMargin,
      e)
  }

  def containsHeader(name: String, value: String) = new Matcher[Response] {
    def apply[S <: Response](e: Expectable[S]): MatchResult[S] = result(
      e.value.headers.toList.map(_.toRaw) contains Header(name, value),
      s"""${e.value.headers}
         |
         |contains
         |
         |$name""".stripMargin,
      s"""The response headers '${e.value.headers.toList.mkString("\n")}'
          |
         |do not contain
          |
         |[$name: $value]
          |""".stripMargin,
      e)
  }
}
