package zio.stream

/** Error type used by [[ZDataSink.readIntLength]] and [[ZDataStream.writeIntLength]] if the `Int` read/provided is
  * negative
  */
case class NegativeLengthException(length: Int) extends Exception(s"Length cannot be negative, but is $length")
