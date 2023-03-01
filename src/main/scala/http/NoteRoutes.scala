package http

import domain.{DecodingException, Note}
import program.CreateNoteProgramAlg
import zhttp.http._
import zio._
import zio.json._

trait NoteRoutesAlg {
  def routes: Http[Any, Throwable, Request, Response]

}

final case class NoteRoutes(
    private val program: CreateNoteProgramAlg
) extends NoteRoutesAlg {

  private val basicRequest: Task[Response] => Task[Response] =
    _.catchSome {
      case e: DecodingException =>
        ZIO.logErrorCause(
          "Failed attempt to decode the json body",
          Cause.fail(e)
        ) *>
          ZIO.attempt(Response.status(Status.BadRequest))
      case e =>
        ZIO.logErrorCause("Something unexpected happened", Cause.fail(e)) *>
          ZIO.attempt(Response.status(Status.InternalServerError))
    }

  private def decodeJsonString[A](
      jsonString: String
  )(implicit jsonDecoder: JsonDecoder[A]): Task[A] =
    ZIO
      .fromEither(jsonString.fromJson[A])
      .mapError(str => DecodingException(str))

  def routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "note" =>
        basicRequest(for {
          _ <- ZIO.logInfo(s"Received request: $req")
          jsonString <- req.body.asString
          note <- decodeJsonString[Note](jsonString)
          noteID <- program.createNote(note)
        } yield Response.json(noteID.toString).setStatus(Status.Created))
      case _ =>
        ZIO.succeed(Response.status(Status.NotFound))
    }

}

object NoteRoutes {
  val live: ZLayer[CreateNoteProgramAlg, Nothing, NoteRoutesAlg] =
    ZLayer.fromFunction(NoteRoutes.apply _)
}
