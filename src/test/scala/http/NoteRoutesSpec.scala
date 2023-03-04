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

      program.provide(
        NoteRoutes.live,
        CreateNoteProgramMock.empty,
        GetNoteProgramMock.empty
      )
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
          GetNoteProgramMock.empty,
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

      program.provide(
        NoteRoutes.live,
        CreateNoteProgramMock.empty,
        GetNoteProgramMock.empty
      )
    },
    test(
      "should return Internal Server Error when something unexpected happens"
    ) {
      val noteGen = Gen.alphaNumericString.map(domain.Note(_, List.empty))
      checkAll(noteGen) { note =>
        val path = Path.decode("/note")

        val mockCreateNoteProgramLayer = CreateNoteProgramMock
          .CreateNote(
            Assertion.equalTo(note),
            Expectation.failureZIO(_ =>
              ZIO.fail(new Exception("Failed to create a note"))
            )
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
          GetNoteProgramMock.empty,
          NoteRoutes.live
        )
      }
    }
  )

  private val getNote = suite("Get /note/:id")(
    test("should return a note") {
      val noteGen = Gen.alphaNumericString.map(domain.Note(_, List.empty))
      checkAll(noteGen, Gen.long) { (note, noteId) =>
        val path = Path.decode(s"/note/$noteId")
        val request: Request = Request(
          url = URL(path)
        )

        val mockGetNoteProgramLayer = GetNoteProgramMock
          .GetNote(
            Assertion.equalTo(noteId),
            Expectation.valueZIO(_ => ZIO.succeed(Some(note)))
          )
          .exactly(1)
          .toLayer

        val program = for {
          route <- ZIO.service[NoteRoutesAlg]
          response <- route.routes(request)
        } yield assertTrue(response.status == Status.Ok)

        program.provide(
          NoteRoutes.live,
          CreateNoteProgramMock.empty,
          mockGetNoteProgramLayer
        )
      }
    },
    test("should return NotFound when note is not found") {
      checkAll(Gen.long) { noteId =>
        val path = Path.decode(s"/note/$noteId")
        val request: Request = Request(
          url = URL(path)
        )

        val mockGetNoteProgramLayer = GetNoteProgramMock
          .GetNote(
            Assertion.equalTo(noteId),
            Expectation.valueZIO(_ => ZIO.succeed(None))
          )
          .exactly(1)
          .toLayer

        val program = for {
          route <- ZIO.service[NoteRoutesAlg]
          response <- route.routes(request)
        } yield assertTrue(response.status == Status.NotFound)

        program.provide(
          NoteRoutes.live,
          CreateNoteProgramMock.empty,
          mockGetNoteProgramLayer
        )
      }
    },
    test(
      "should return InternalServerError when something unexpected happens"
    ) {
      checkAll(Gen.long) { noteId =>
        val path = Path.decode(s"/note/$noteId")
        val request: Request = Request(
          url = URL(path)
        )

        val mockGetNoteProgramLayer = GetNoteProgramMock
          .GetNote(
            Assertion.equalTo(noteId),
            Expectation.failureZIO(_ =>
              ZIO.fail(new Exception("Failed to get a note"))
            )
          )
          .exactly(1)
          .toLayer

        val program = for {
          route <- ZIO.service[NoteRoutesAlg]
          response <- route.routes(request)
        } yield assertTrue(response.status == Status.InternalServerError)

        program.provide(
          NoteRoutes.live,
          CreateNoteProgramMock.empty,
          mockGetNoteProgramLayer
        )
      }
    }
  )

  def spec = suite("NoteRoutes")(
    noteCreation,
    getNote,
    basicRouteFailureCases
  )
}
