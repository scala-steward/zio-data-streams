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

import zio.ChunkBuilder
import zio.stream.ZStream

import java.io.UTFDataFormatException

/**
 * Streams to interoperate with JDK's [[java.io.DataInput]] and
 * [[java.io.DataOutput]] format in a way analogous to JDK's
 * [[java.io.DataOutputStream]].
 *
 * Generally, Streams are provided for each method of the [[java.io.DataOutput]]
 * interface, with these exceptions:
 *
 *   - `void write(int b)`: Use `ZStream(b.toByte)` instead
 *   - `void write(byte[] b)`: Use `ZStream.fromChunk(Chunk.fromArray(b))`
 *     instead
 *   - `void write(byte[] b, int off, int len)`: Use
 *     `ZStream.fromChunk(Chunk.fromArray(b).drop(off).take(len))` instead
 *
 * @see
 *   https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/DataOutput.html
 */
object ZDataStream {

  val ZERO: Byte = 0.toByte
  val ONE: Byte  = 1.toByte

  /** Outputs one Byte, either `0x01` if its argument is `true`, else `0x00`. */
  def writeBoolean(v: Boolean): ZStream[Any, Nothing, Byte] =
    if (v) ZStream(ONE) else ZStream(ZERO)

  /**
   * Outputs one Byte for each Char in the String.
   *
   * Caution: The number of Bytes being output is not output first, so this is
   * not reversible by [[ZDataSink]] without another way of knowing how many
   * Bytes need to be read.
   *
   * Caution: This loses information. It only writes the low-order Byte of each
   * Char in the String.
   */
  def writeBytes(s: String): ZStream[Any, Nothing, Byte] =
    ZStream.fromIterable(s.map(c => (c & 0xff).toByte))

  /**
   * Outputs two Bytes, first the high-order Byte of the Char, then the
   * low-order Byte.
   */
  def writeChar(v: Char): ZStream[Any, Nothing, Byte] = {
    val b0 = ((v >>> 8) & 0xff).toByte
    val b1 = ((v >>> 0) & 0xff).toByte
    ZStream(b0, b1)
  }

  /**
   * Outputs two Bytes for each Char in the String.
   *
   * Caution: The number of Bytes or Chars being output is not output first, so
   * this is not reversible by [[ZDataSink]] without another way of knowing how
   * many Chars need to be read.
   */
  def writeChars(s: String): ZStream[Any, Nothing, Byte] =
    ZStream.fromIterable(s).flatMap(ZDataStream.writeChar)

  /**
   * Outputs eight Bytes: the Bytes of the Long returned by
   * `java.lang.Double.doubleToLongBits(v)`, in big-endian order.
   */
  def writeDouble(v: Double): ZStream[Any, Nothing, Byte] =
    writeLong(java.lang.Double.doubleToLongBits(v))

  /**
   * Outputs four Bytes: the Bytes of the Int returned by
   * `java.lang.Float.floatToIntBits(v)`, in big-endian order.
   */
  def writeFloat(v: Float): ZStream[Any, Nothing, Byte] =
    writeInt(java.lang.Float.floatToIntBits(v))

  /**
   * Outputs four Bytes: the Bytes of `v`, in big-endian order.
   */
  def writeInt(v: Int): ZStream[Any, Nothing, Byte] = {
    val b0 = ((v >>> 24) & 0xff).toByte
    val b1 = ((v >>> 16) & 0xff).toByte
    val b2 = ((v >>> 8) & 0xff).toByte
    val b3 = ((v >>> 0) & 0xff).toByte
    ZStream(b0, b1, b2, b3)
  }

  /**
   * Outputs four Bytes: the Bytes of `v`, in big-endian order, except if v is
   * negative in which case it fails with NegativeLengthException.
   */
  def writeIntLength(v: Int): ZStream[Any, NegativeLengthException, Byte] =
    if (v < 0) ZStream.fail(NegativeLengthException(v)) else writeInt(v)

  /**
   * Outputs eight Bytes: the Bytes of `v`, in big-endian order.
   */
  def writeLong(v: Long): ZStream[Any, Nothing, Byte] = {
    val b0 = ((v >>> 56) & 0xff).toByte
    val b1 = ((v >>> 48) & 0xff).toByte
    val b2 = ((v >>> 40) & 0xff).toByte
    val b3 = ((v >>> 32) & 0xff).toByte
    val b4 = ((v >>> 24) & 0xff).toByte
    val b5 = ((v >>> 16) & 0xff).toByte
    val b6 = ((v >>> 8) & 0xff).toByte
    val b7 = ((v >>> 0) & 0xff).toByte
    ZStream(b0, b1, b2, b3, b4, b5, b6, b7)
  }

  def writeModifiedUTF8(str: String): ZStream[Any, UTFDataFormatException, Byte] = {
    val strlen = str.length
    var utflen = strlen // optimized for ASCII

    for (i <- 0 until strlen) {
      val c = str.charAt(i)
      if (c >= 0x80 || c == 0) {
        utflen += (if (c >= 0x800) 2 else 1)
      }
    }

    if (utflen > 65535 || /* overflow */ utflen < strlen) {
      val slen = str.length
      val head = str.substring(0, 8)
      val tail = str.substring(slen - 8, slen)
      // handle int overflow with max 3x expansion
      val actualLength = slen.toLong + Integer.toUnsignedLong(utflen - slen)
      val msg          = "encoded string (" + head + "..." + tail + ") too long: " + actualLength + " bytes"
      ZStream.fail(new UTFDataFormatException(msg))
    } else {
      val cb = ChunkBuilder.make[Byte](utflen + 2) // Two extra for unsigned short length
      // write two-byte unsigned short length
      cb.addOne(((utflen >>> 8) & 0xff).toByte)
      cb.addOne(((utflen >>> 0) & 0xff).toByte)
      for (c <- str) {
        c match {
          case b if b < 0x80 && b != 0 => // ASCII and non-zero
            cb.addOne(b.toByte)
          case bbb if bbb >= 0x800 => {
            cb.addOne((0xe0 | ((c >> 12) & 0x0f)).toByte)
            cb.addOne((0x80 | ((c >> 6) & 0x3f)).toByte)
            cb.addOne((0x80 | ((c >> 0) & 0x3f)).toByte)
          }
          case bb @ _ => { // Zero is written as two non-zero Bytes
            cb.addOne((0xc0 | ((c >> 6) & 0x1f)).toByte)
            cb.addOne((0x80 | ((c >> 0) & 0x3f)).toByte)
          }
        }
      }

      ZStream.fromChunk(cb.result())
    }
  }

  /**
   * Outputs two Bytes: the Bytes of `v`, in big-endian order.
   */
  def writeShort(v: Short): ZStream[Any, Nothing, Byte] = {
    val b0 = ((v >>> 8) & 0xff).toByte
    val b1 = ((v >>> 0) & 0xff).toByte
    ZStream(b0, b1)
  }

}
