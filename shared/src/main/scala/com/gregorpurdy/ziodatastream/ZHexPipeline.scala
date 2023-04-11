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

import zio.stream.{ZChannel, ZPipeline}
import zio.{Chunk, ChunkBuilder, ZIO}

import scala.annotation.unused

object ZHexPipeline {

  /**
   * Decode each pair of hex digit input bytes (both lower or upper case letters
   * are allowed) as one output byte.
   */
  def hexDecode: ZPipeline[Any, EncodingException, Byte, Byte] = {
    def digitValue(b: Byte): Int = b match {
      case d if d >= '0' && d <= '9' => d - '0'
      case l if l >= 'a' && l <= 'f' => 10 + l - 'a'
      case u if u >= 'A' && u <= 'F' => 10 + u - 'A'
      case _                         => -1
    }
    def decodeChannel(
      spare: Chunk[Byte]
    ): ZChannel[Any, Nothing, Chunk[Byte], Any, EncodingException, Chunk[Byte], Unit] = {
      def in(in: Chunk[Byte]): ZChannel[Any, Nothing, Chunk[Byte], Any, EncodingException, Chunk[Byte], Unit] = {
        val toProcess = spare ++ in
        if (toProcess.isEmpty) {
          ZChannel.unit
        } else {
          val l = toProcess.size
          val (bs, newSpare) = if (l % 2 == 0) {
            (toProcess, Chunk.empty[Byte])
          } else {
            toProcess.splitAt(l - 1)
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
            case None    => ZChannel.write(temp.result()) *> decodeChannel(newSpare)
            case Some(e) => ZChannel.fail(EncodingException(s"Not a valid hex digit: '${e.toChar}'"))
          }
        }
      }
      def err(z: Nothing): ZChannel[Any, Nothing, Chunk[Byte], Any, EncodingException, Chunk[Byte], Unit] =
        throw new UnsupportedOperationException("Input stream should be infallible")

      def done(@unused u: Any): ZChannel[Any, Nothing, Chunk[Byte], Any, EncodingException, Chunk[Byte], Unit] =
        if (spare.isEmpty) {
          ZChannel.succeed(())
        } else {
          ZChannel.fail(EncodingException("Extra input at end after last fully encoded byte"))
        }

      ZChannel.readWith[Any, Nothing, zio.Chunk[Byte], Any, EncodingException, zio.Chunk[Byte], Unit](
        in,
        err,
        done
      )
    }

    ZPipeline.fromChannel(decodeChannel(Chunk.empty[Byte]))
  }

  /**
   * Encode each input byte as two output bytes as the hex representation of the
   * input byte.
   */
  def hexEncode: ZPipeline[Any, Nothing, Byte, Byte] = {
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
    ZPipeline.fromPush[Any, Nothing, Byte, Byte](
      ZIO.succeed((inChunkOpt: Option[Chunk[Byte]]) =>
        inChunkOpt match {
          case None => ZIO.succeed(Chunk.empty[Byte])
          case Some(bs) =>
            ZIO.succeed {
              val out = ChunkBuilder.make[Byte](bs.size * 2)
              for (b <- bs) {
                out += DIGITS((b >>> 4) & 0x0f)
                out += DIGITS((b >>> 0) & 0x0f)
              }
              out.result()
            }
        }
      )
    )
  }

}
