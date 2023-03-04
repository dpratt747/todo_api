package http

import domain.{DecodingException, Note, NoteNotFoundException}
import program.{CreateNoteProgramAlg, GetNoteProgramAlg}
import zhttp.http._
import zio._
import zio.json._

trait NoteRoutesAlg {
  def routes: Http[Any, Throwable, Request, Response]

}

final case class NoteRoutes(
    private val createNoteProgram: CreateNoteProgramAlg,
    private val getNoteProgram: GetNoteProgramAlg
) extends NoteRoutesAlg {

  private val basicRequest: Task[Response] => Task[Response] =
    _.catchSome {
      case e: DecodingException =>
        ZIO.logErrorCause(
          "Failed attempt to decode the json body",
          Cause.fail(e)
        ) *>
          ZIO.attempt(Response.status(Status.BadRequest))
      case e: NoteNotFoundException =>
        ZIO.logErrorCause(
          "Failed attempt to retrieve the note",
          Cause.fail(e)
        ) *>
          ZIO.attempt(Response.status(Status.NotFound))
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
      case req @ Method.POST -> Path.root / "note" =>
        basicRequest(for {
          _ <- ZIO.logInfo(s"Received request: $req")
          jsonString <- req.body.asString
          note <- decodeJsonString[Note](jsonString)
          noteID <- createNoteProgram.createNote(note)
        } yield Response.json(noteID.toString).setStatus(Status.Created))
      case req @ Method.GET -> Path.root / "note" / noteID =>
        basicRequest(for {
          _ <- ZIO.logInfo(s"Received request: $req")
          noteO <- getNoteProgram.getNote(noteID.toLong)
          note <- ZIO
            .fromOption(noteO)
            .orElseFail(NoteNotFoundException("Note not found"))
        } yield Response.json(note.toJson).setStatus(Status.Ok))
      case _ =>
        ZIO.succeed(Response.status(Status.NotFound))
    }

}

object NoteRoutes {
  val live: ZLayer[
    CreateNoteProgramAlg with GetNoteProgramAlg,
    Nothing,
    NoteRoutesAlg
  ] =
    ZLayer.fromFunction(NoteRoutes.apply _)
}
