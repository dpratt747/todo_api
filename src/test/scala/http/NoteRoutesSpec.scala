package http

import util.mocks._
import zhttp.http._
import zio.test._
import zio._
import zio.json._
import zio.mock._

object NoteRoutesSpec extends ZIOSpecDefault {

  private val basicRouteFailureCases = suite("Basic failure cases")(
    test("should return 404 when path is not found") {
      val path = Path.decode("/note-found")
      val request: Request = Request(
        url = URL(path)
      )

      val program = for {
        route <- ZIO.service[NoteRoutesAlg]
        response <- route.routes(request)
      } yield assertTrue(response.status == Status.NotFound)

      program.provide(NoteRoutes.live, CreateNoteProgramMock.empty)
    }
  )

  private val noteCreation = suite("Post /note")(
    test("should create a note") {
      val noteGen = Gen.alphaNumericString.map(domain.Note(_, List.empty))
      checkAll(noteGen) { note =>
        val path = Path.decode("/note")

        val mockCreateNoteProgramLayer = CreateNoteProgramMock
          .CreateNote(
            Assertion.equalTo(note),
            Expectation.valueZIO(_ => ZIO.succeed(1L))
          )
          .exactly(1)
          .toLayer

        val request: Request = Request(
          method = Method.POST,
          url = URL(path),
          body = Body.fromString(
            note.toJson
          )
        )

        val program = for {
          route <- ZIO.service[NoteRoutesAlg]
          response <- route.routes(request)
        } yield assertTrue(response.status == Status.Created)

        program.provide(
          mockCreateNoteProgramLayer,
          NoteRoutes.live
        )
      }
    },
    test("should return BadRequest when json is not valid") {
      val path = Path.decode("/note")
      val request: Request = Request(
        method = Method.POST,
        url = URL(path),
        body = Body.empty
      )

      val program = for {
        route <- ZIO.service[NoteRoutesAlg]
        response <- route.routes(request)
      } yield assertTrue(response.status == Status.BadRequest)

      program.provide(NoteRoutes.live, CreateNoteProgramMock.empty)
    },
    test("should return Internal Server Error when something unexpected happens") {
      val noteGen = Gen.alphaNumericString.map(domain.Note(_, List.empty))
      checkAll(noteGen) { note =>
        val path = Path.decode("/note")

        val mockCreateNoteProgramLayer = CreateNoteProgramMock
          .CreateNote(
            Assertion.equalTo(note),
            Expectation.failureZIO(_ => ZIO.fail(new Exception("Failed to create a note")))
          )
          .exactly(1)
          .toLayer

        val request: Request = Request(
          method = Method.POST,
          url = URL(path),
          body = Body.fromString(
            note.toJson
          )
        )

        val program = for {
          route <- ZIO.service[NoteRoutesAlg]
          response <- route.routes(request)
        } yield assertTrue(response.status == Status.InternalServerError)

        program.provide(
          mockCreateNoteProgramLayer,
          NoteRoutes.live
        )
      }
    }
  )

  def spec = suite("NoteRoutes")(
    noteCreation,
    basicRouteFailureCases
  )
}
