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

package zio.stream

import java.io.EOFException

import scala.annotation.unused

import zio._

object ZHexPipeline {

  /** ASCII characters for hex digits */
  val DIGITS: Array[Byte] = Array(
    '0'.toByte,
    '1'.toByte,
    '2'.toByte,
    '3'.toByte,
    '4'.toByte,
    '5'.toByte,
    '6'.toByte,
    '7'.toByte,
    '8'.toByte,
    '9'.toByte,
    'a'.toByte,
    'b'.toByte,
    'c'.toByte,
    'd'.toByte,
    'e'.toByte,
    'f'.toByte
  )

  /**
   * Integer value of a hex digit, allowing both upper and lower case for the
   * letters.
   *
   * @return
   *   the digit's value, or -1 if the input Byte is not a valid ASCII hex
   *   character
   */
  private def digitValue(b: Byte): Int = b match {
    case d if d >= '0' && d <= '9' => d - '0'
    case l if l >= 'a' && l <= 'f' => 10 + l - 'a'
    case u if u >= 'A' && u <= 'F' => 10 + u - 'A'
    case _                         => -1
  }

  def decodeChannel: ZChannel[Any, Nothing, Chunk[Byte], Any, HexDecodeException, Chunk[Byte], Unit] = {
    var spare: Chunk[Byte] = Chunk.empty[Byte]
    def in(in: Chunk[Byte]): ZChannel[Any, Nothing, Chunk[Byte], Any, HexDecodeException, Chunk[Byte], Unit] = {
      val toProcess = spare ++ in
      if (toProcess.isEmpty) {
        ZChannel.unit
      } else {
        val l = toProcess.size
        val bs = if (l % 2 == 0) {
          toProcess
        } else {
          val (front, tail) = toProcess.splitAt(l - 1)
          spare = tail
          front
        }
        val temp: ChunkBuilder[Byte] = ChunkBuilder.make[Byte](l / 2)
        var bad: Option[Byte]        = None
        for (i <- 0 until bs.size by 2) {
          if (bad.isEmpty) {
            val h = digitValue(bs(i))
            if (h < 0) {
              bad = Some(bs(i))
            } else {
              val l = digitValue(bs(i + 1))
              if (l < 0) {
                bad = Some(bs(i + 1))
              } else {
                val v = (h << 4) | l
                val b = v.toByte
                temp += b
              }
            }
          }
        }
        bad match {
          case None    => ZChannel.write(temp.result())
          case Some(e) => ZChannel.fail(HexDecodeException.InvalidHexCharException(e.toChar))
        }
      }
    }
    def err(z: Nothing): ZChannel[Any, Nothing, Chunk[Byte], Any, HexDecodeException, Chunk[Byte], Unit] =
      throw new UnsupportedOperationException("Input stream should be infallible")
    def done(@unused u: Any): ZChannel[Any, Nothing, Chunk[Byte], Any, HexDecodeException, Chunk[Byte], Unit] = if (
      spare.isEmpty
    ) {
      ZChannel.succeed(())
    } else {
      ZChannel.fail(HexDecodeException.IncompleteByteException)
    }

    ZChannel.readWith(in, err, done)
  }

  def decode: ZPipeline[Any, HexDecodeException, Byte, Byte] = ZPipeline.fromChannel(decodeChannel)

  def encodeChunk(in: Chunk[Byte]): Chunk[Byte] = {
    val out = ChunkBuilder.make[Byte](in.size * 2)
    for (b <- in) {
      out += DIGITS((b >>> 4) & 0x0f)
      out += DIGITS((b >>> 0) & 0x0f)
    }
    out.result()
  }

  def encodeChunkOpt(inChunkOpt: Option[Chunk[Byte]]): Chunk[Byte] = inChunkOpt match {
    case None     => Chunk.empty[Byte]
    case Some(bs) => encodeChunk(bs)
  }

  def encodeChunkOptZIO(inChunkOpt: Option[Chunk[Byte]]): ZIO[Any, Nothing, Chunk[Byte]] =
    ZIO.succeed(encodeChunkOpt(inChunkOpt))

  def encode: ZPipeline[Any, Nothing, Byte, Byte] =
    ZPipeline.fromPush[Any, Nothing, Byte, Byte](ZIO.succeed(encodeChunkOptZIO))

}
