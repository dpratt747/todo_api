package http

import domain.Note
import program._
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
          response <- ZIO
            .fromOption(noteO)
            .fold(
              _ => Response.json("Note not found").setStatus(Status.NotFound),
              note => Response.json(note.toJson).setStatus(Status.Ok)
            )
        } yield response)
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
