package zio.stream

sealed trait HexDecodeException extends Exception

object HexDecodeException {
  case class InvalidHexCharException(c: Char) extends HexDecodeException

  /**
   * Incomplete byte at end of input
   */
  case object IncompleteByteException extends HexDecodeException
}
