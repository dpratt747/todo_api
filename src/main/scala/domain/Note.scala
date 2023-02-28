package domain

import zio.json._

final case class Note(text: String, tags: List[String])

object Note {

  implicit val decoder: JsonDecoder[Note] =
    DeriveJsonDecoder.gen[Note]
  implicit val encoder: JsonEncoder[Note] =
    DeriveJsonEncoder.gen[Note]

}
