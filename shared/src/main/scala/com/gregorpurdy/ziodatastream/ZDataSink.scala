/*
 * Copyright 2022 Gregor Purdy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gregorpurdy.ziodatastream

import zio.stream.ZSink
import zio.{Chunk, ChunkBuilder, ZIO}

import java.io.{EOFException, UTFDataFormatException}
import scala.annotation.tailrec

/**
 * Sinks to interoperate with JDK's [[java.io.DataInput]] and
 * [[java.io.DataOutput]] format in a way analogous to JDK's
 * [[java.io.DataInputStream]].
 *
 * Note: The JDK's interfaces define methods that operate on mutable data
 * (`Byte` arrays passed in as arguments). ZIO Streams and Sinks are meant for
 * immutable data and use [[zio.Chunk]] instead.
 *
 * Generally, Sinks are provided for each method of the [[java.io.DataInput]]
 * interface, with these exceptions:
 *
 *   - `void readFully(byte[] b)`: Obtain a `Chunk[Byte]` via `ZSink.take(n)`
 *     (where `n` is `b.length`) instead
 *   - `void readFully(byte[] b, int off, int len)`: Obtain a `Chunk[Byte]` via
 *     `ZSink.take(len)` instead
 *
 * And this one under a different name:
 *
 *   - `String readUTF()`: Renamed `readModifiedUTF8`
 *
 * There are an additional public utility Sinks for reading lengths and Chunks
 * not present in [[java.io.DataInput]]:
 *
 *   - `readIntLength` that reads an `Int` length and ensures it is
 *     non-negative.
 *   - `readIntLengthChunk` that reads an `Int` length and ensures it is
 *     non-negative.
 *   - `readN` that reads using a given Sink `n` times, producing a Chunk as
 *     output.
 *   - `readUnsignedShortLength` that reads a two-Byte `Int` length and ensures
 *     it is non-negative.
 *   - `readUnsignedShortLengthChunk` that reads a two-Byte length and then
 *     reads and returns that many Bytes in a Chunk
 *   - `readUntil` that reads using a given Sink until the result matches a
 *     predicate, omitting final read result
 *
 * To do's:
 *   - TODO: readLine()
 *   - TODO: skipBytes() -- Is there a version of ZSink.drop() that we could
 *     use?
 *   - TODO: Use EOFException as error type everywhere, or... what?
 *     ZSink.foldWhile can error out due to EOF but does not declare that in its
 *     type...
 *
 * @see
 *   https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/DataInput.html
 */
object ZDataSink {

  /**
   * Consumes one `Byte` and succeeds with `false` if zero, or `true` if
   * non-zero. Fails with [[java.io.EOFException]] if unable to consume a
   * `Byte`.
   */
  def readBoolean: ZSink[Any, EOFException, Byte, Byte, Boolean] =
    ZSink.head[Byte].flatMap {
      _ match {
        case Some(b) => ZSink.succeed(b != 0)
        case None    => ZSink.fail(new EOFException)
      }
    }

  /**
   * Consumes one `Byte` and succeeds with its value. Fails with
   * [[java.io.EOFException]] if unable to consume a `Byte`.
   */
  def readByte: ZSink[Any, EOFException, Byte, Byte, Byte] =
    ZSink.head[Byte].flatMap {
      case Some(b) => ZSink.succeed(b)
      case None    => ZSink.fail(new EOFException)
    }

  /**
   * Consumes two `Byte`s and succeeds with their (big-endian) `Char` value.
   * Fails with [[java.io.EOFException]] if < 2 `Byte`s are available.
   */
  def readChar: ZSink[Any, Nothing, Byte, Byte, Char] =
    readUnsignedShort.map(_.toChar)

  /**
   * Consumes eight `Byte`s and succeeds with their (big-endian) `Long` value
   * converted by [[java.lang.Double.longBitsToDouble]]. Fails
   * with[[java.io.EOFException]] if < 8 `Byte`s are available.
   */
  def readDouble: ZSink[Any, Nothing, Byte, Byte, Double] =
    readLong.map(java.lang.Double.longBitsToDouble)

  /**
   * Consumes four `Byte`s and succeeds with their (big-endian) `Int` value
   * converted by [[java.lang.Float.intBitsToFloat]]. Fails with
   * [[java.io.EOFException]] if < 4 `Byte`s are available.
   */
  def readFloat: ZSink[Any, Nothing, Byte, Byte, Float] =
    readInt.map(java.lang.Float.intBitsToFloat)

  /**
   * Consumes four `Byte`s and succeeds with their (big-endian) `Int` value.
   * Fails with [[java.io.EOFException]] if < 4 `Byte`s are available.
   */
  def readInt: ZSink[Any, Nothing, Byte, Byte, Int] =
    ZSink.foldUntil[Byte, Int](0, 4)((v, b) => (v << 8) + (b & 255))

  /**
   * Consumes four `Byte`s and succeeds with their (big-endian) `Int` value, or
   * fails with [[NegativeLengthException]] if the value is negative. Fails with
   * [[java.io.EOFException]] if < 4 `Byte`s are available.
   */
  def readIntLength: ZSink[Any, NegativeLengthException, Byte, Byte, Int] = for {
    length <- readInt
    valid  <- if (length < 0) ZSink.fail(NegativeLengthException(length)) else ZSink.succeed(length)
  } yield valid

  /** Read an `Int` length prefixed Chunk of Bytes */
  def readIntLengthChunk: ZSink[Any, NegativeLengthException, Byte, Byte, Chunk[Byte]] = for {
    length <- readIntLength
    chunk  <- ZSink.take[Byte](length)
  } yield chunk

  /**
   * Consumes eight `Byte`s and succeeds with their (big-endian) `Long` value.
   * Fails with [[java.io.EOFException]] if < 8 `Byte`s are available.
   */
  def readLong: ZSink[Any, Nothing, Byte, Byte, Long] =
    ZSink.foldUntil[Byte, Long](0L, 8)((v, b) => (v << 8) + (b & 255).toLong)

  /**
   * Reads via given Sink an integer number of times, returning a Chunk of
   * values read.
   */
  def readN[A, E](
    n: Int,
    sinkF: () => ZSink[Any, E, Byte, Byte, A]
  ): ZSink[Any, E, Byte, Byte, Chunk[A]] = {
    def readNRec(cb: ChunkBuilder[A], n: Int): ZSink[Any, E, Byte, Byte, Chunk[A]] =
      if (n < 1) {
        ZSink.succeed(cb.result())
      } else {
        sinkF().flatMap { v =>
          cb.addOne(v)
          readNRec(cb, n - 1)
        }
      }

    readNRec(ChunkBuilder.make[A](n), n)
  }

  /**
   * Reads via given Sink until the resulting value matches a predicate,
   * returning a Chunk of values read (not including the final one).
   */
  def readUntil[A, E](p: A => Boolean)(
    sinkF: () => ZSink[Any, E, Byte, Byte, A]
  ): ZSink[Any, E, Byte, Byte, Chunk[A]] = {
    def readUntilRec(cb: ChunkBuilder[A]): ZSink[Any, E, Byte, Byte, Chunk[A]] =
      sinkF().flatMap { v =>
        if (p(v)) {
          ZSink.succeed(cb.result())
        } else {
          cb.addOne(v)
          readUntilRec(cb)
        }
      }

    readUntilRec(ChunkBuilder.make[A]())
  }

  /**
   * Consumes two `Byte`s and succeeds with their (big-endian) `Short` value.
   * Fails with [[java.io.EOFException]] if < 2 `Byte`s are available.
   */
  def readShort: ZSink[Any, Nothing, Byte, Byte, Short] =
    readUnsignedShort.map(_.toShort)

  /**
   * Consumes one `Byte` and succeeds with its unsigned value as an Int. Fails
   * with [[java.io.EOFException]] if unable to consume a `Byte`.
   */
  def readUnsignedByte: ZSink[Any, EOFException, Byte, Byte, Int] =
    ZSink.head[Byte].flatMap {
      case Some(b) => ZSink.succeed(b & 255)
      case None    => ZSink.fail(new EOFException)
    }

  /**
   * Consumes two `Byte`s and succeeds with their (big-endian) `Short` unsigned
   * value as an Int. Fails with [[java.io.EOFException]] if < 2 `Byte`s are
   * available.
   */
  def readUnsignedShort: ZSink[Any, Nothing, Byte, Byte, Int] =
    ZSink.foldUntil[Byte, Int](0, 2)((v, b) => (v << 8) + (b & 255))

  /** Read an unsigned `Short` length prefixed Chunk of Bytes */
  def readUnsignedShortLengthChunk: ZSink[Any, Nothing, Byte, Byte, Chunk[Byte]] = for {
    length <- readUnsignedShort
    chunk  <- ZSink.take[Byte](length)
  } yield chunk

  /** Read a "modified" UTF8 String, prefixed by an unsigned Short byte count */
  def readModifiedUTF8: ZSink[Any, UTFDataFormatException, Byte, Byte, String] = {
    @tailrec
    def asciiPrefix(cb: ChunkBuilder[Char], count: Int, length: Int, bytes: Chunk[Byte]): Int =
      if (count < length) {
        val c = (bytes(count) & 0xff)
        if (c > 127) count
        else {
          cb.addOne(c.toChar)
          asciiPrefix(cb, count + 1, length, bytes)
        }
      } else {
        count
      }

    @tailrec
    def mutf8Suffix(
      cb: ChunkBuilder[Char],
      count: Int,
      length: Int,
      bytes: Chunk[Byte]
    ): ZIO[Any, UTFDataFormatException, Unit] =
      if (count < length) {
        val c = (bytes(count) & 0xff)
        (c >> 4) match {
          case n if n < 8 => { /* 0xxxxxxx: ASCII */
            cb.addOne(c.toChar)
            mutf8Suffix(cb, count + 1, length, bytes)
          }
          case n if n == 12 || n == 13 => { /* 110x xxxx   10xx xxxx */
            val newCount = count + 2
            if (newCount > length) {
              ZIO.fail(new UTFDataFormatException("malformed input: partial character at end"))
            } else {
              val c2 = (bytes(count + 1) & 0xff)
              if ((c2 & 0xc0) != 0x80) {
                ZIO.fail(new UTFDataFormatException("malformed input around byte " + count))
              } else {
                val newChar = (((c & 0x1f) << 6) | (c2 & 0x3f)).toChar
                cb.addOne(newChar)
                mutf8Suffix(cb, newCount, length, bytes)
              }
            }
          }
          case 14 => { /* 1110 xxxx  10xx xxxx  10xx xxxx */
            val newCount = count + 3
            if (newCount > length) {
              ZIO.fail(new UTFDataFormatException("malformed input: partial character at end"))
            } else {
              val c2 = (bytes(count + 1) & 0xff)
              val c3 = (bytes(count + 2) & 0xff)
              if (((c2 & 0xc0) != 0x80) || ((c3 & 0xc0) != 0x80)) {
                ZIO.fail(new UTFDataFormatException("malformed input around byte " + (count - 1)))
              } else {
                val newChar = (((c & 0x0f) << 12) | ((c2 & 0x3f) << 6) | ((c3 & 0x3f) << 0)).toChar
                cb.addOne(newChar)
                mutf8Suffix(cb, newCount, length, bytes)
              }
            }
          }
          case _ => { /* 10xx xxxx,  1111 xxxx */
            ZIO.fail(new UTFDataFormatException("malformed input around byte " + count))
          }
        }
      } else {
        ZIO.succeed(())
      }

    readUnsignedShortLengthChunk.mapZIO { bytes =>
      val cb     = ChunkBuilder.make[Char]()
      val length = bytes.size
      if (length == 0) {
        ZIO.succeed("")
      } else {
        val count = asciiPrefix(cb, 0, length, bytes)
        for {
          _ <- mutf8Suffix(cb, count, length, bytes)
        } yield new String(cb.result().toArray)
      }
    }
  }

}
