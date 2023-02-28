package domain

final case class DecodingException(message: String) extends Exception(message)
