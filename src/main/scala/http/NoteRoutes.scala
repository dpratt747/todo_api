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
          ZIO.attempt(Response.json(e.message).setStatus(Status.BadRequest))
      case e: NoteNotFoundException =>
        ZIO.logErrorCause(
          "Failed attempt to retrieve the note",
          Cause.fail(e)
        ) *>
          ZIO.attempt(Response.json(e.message).setStatus(Status.NotFound))
      case e =>
        ZIO.logErrorCause("Something unexpected happened", Cause.fail(e)) *>
          ZIO.attempt(Response.status(Status.InternalServerError))
    }

  def routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> Path.root / "note" =>
        basicRequest(for {
          jsonString <- req.body.asString
          note <- decodeJsonString[Note](jsonString)
          noteID <- createNoteProgram.createNote(note)
        } yield Response.json(noteID.toString).setStatus(Status.Created))
      case Method.GET -> Path.root / "note" / long(noteID) =>
        basicRequest(for {
          noteO <- getNoteProgram.getNote(noteID)
          note <- ZIO
            .fromOption(noteO)
            .orElseFail(NoteNotFoundException("Note not found"))
        } yield Response.json(note.toJson).setStatus(Status.Ok))
      case _ =>
        ZIO.succeed(Response.status(Status.NotFound))
    } @@ logRequestResponse

}

object NoteRoutes {
  val live: ZLayer[
    CreateNoteProgramAlg with GetNoteProgramAlg,
    Nothing,
    NoteRoutesAlg
  ] =
    ZLayer.fromFunction(NoteRoutes.apply _)
}
