package domain

final case class NoteNotFoundException(message: String) extends Exception(message)
