import zhttp.http._
import zio._
import zio.json._
import domain._
package object http {

  val logRequestResponse
      : Middleware[Any, Nothing, Request, Response, Request, Response] =
    Middleware.transformZIO[Request, Response](
      in => ZIO.logInfo(s"Received request: $in") *> ZIO.succeed(in),
      out => ZIO.logInfo(s"Sending response: $out") *> ZIO.succeed(out)
    )

  def decodeJsonString[A](
      jsonString: String
  )(implicit jsonDecoder: JsonDecoder[A]): Task[A] =
    ZIO
      .fromEither(jsonString.fromJson[A])
      .mapError(str => DecodingException(str))

}
