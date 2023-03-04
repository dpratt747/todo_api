import domain.CustomTypes._
import zhttp.http._
import zio._
import zio.json._
import domain._
package object http {
  object decodeNoteId extends RouteDecode(str => NoteId.apply(str.toLong))

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

  val basicRequest: Task[Response] => Task[Response] =
    _.catchSome {
      case e: DecodingException =>
        ZIO.logErrorCause(
          "Failed attempt to decode the json body",
          Cause.fail(e)
        ) *>
          ZIO.attempt(Response.json(e.message).setStatus(Status.BadRequest))
      case e =>
        ZIO.logErrorCause("Something unexpected happened", Cause.fail(e)) *>
          ZIO.attempt(Response.status(Status.InternalServerError))
    }

}
