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

import zio._
import zio.stream.ZSink
import zio.test._
import zio.test.Assertion._

import java.io._
import java.nio.charset.StandardCharsets

object ZDataSinkSpec extends ZIOSpecDefault {

  private def streamForDOSOp(f: DataOutputStream => Unit): ZStream[Any, Nothing, Byte] = {
    val bos = new ByteArrayOutputStream
    val os  = new DataOutputStream(bos)
    f(os)
    os.flush()
    val bytes = bos.toByteArray
    val oc    = Chunk.fromArray(bytes)
    ZStream.fromChunk(oc)
  }

  private val DIGITS = "0123456789abcdef"

  private def hexForBytesOfString(s: String): String = {
    val bs = s.getBytes(StandardCharsets.UTF_8)
    val sb = new StringBuilder(bs.size * 2)
    for (b <- bs) {
      sb.append(DIGITS((b & 0xf0) >>> 4))
      sb.append(DIGITS((b & 0x0f) >>> 4))
    }
    sb.result
  }

  private def decodeHexDigit(c: Char): Int = c match {
    case d if d >= '0' && d <= '9' => d - '0'
    case l if l >= 'a' && l <= 'f' => l - 'a' + 10
    case u if u >= 'A' && u <= 'F' => u - 'A' + 10
    case _                         => throw new IllegalArgumentException(s"'$c' is not a hex digit")
  }

  private def chunkForHex(s: String): Chunk[Byte] = {
    if (s.size % 2 != 0) {
      throw new IllegalArgumentException(s"String must be even number of hex digits but is ${s.length} characters long")
    }
    val cb = ChunkBuilder.make[Byte](s.length / 2)
    for (temp <- s.grouped(2)) {
      cb.addOne((decodeHexDigit(temp(0)) * 16 + decodeHexDigit(temp(1))).toByte)
    }
    cb.result
  }

  private def stringForHex(s: String): String = new String(chunkForHex(s).toArray, StandardCharsets.UTF_8)

  private def checkValueVsDOS[A](
    expected: A,
    f: (DataOutputStream, A) => Unit,
    sink: ZSink[Any, Any, Byte, Byte, A]
  ) =
    streamForDOSOp(dos => f(dos, expected))
      .run(sink)
      .flatMap { v =>
        if (expected.isInstanceOf[String] && v != expected) {
          println(s"Mismatch for '$expected':")
          println(s"  Expected Hex: \"${hexForBytesOfString(expected.asInstanceOf[String])}\"")
          println(s"  Output Hex: \"${hexForBytesOfString(v.asInstanceOf[String])}\"")
        }
        assertTrue(v == expected)
      }

  private def checkVsDOS[A](gen: Gen[Any, A], f: (DataOutputStream, A) => Unit, sink: ZSink[Any, Any, Byte, Byte, A]) =
    check(gen)(expected => checkValueVsDOS(expected, f, sink))

  def spec = suite("ZDataSinkSpec")(
    test("readInt works for 1") {
      val stream = ZStream.fromIterable("00000001".getBytes).via(ZHexPipeline.decode)
      for {
        value <- stream.run(ZDataSink.readInt)
      } yield assertTrue(value == 1)
    },
    test("readLong works for 1") {
      val stream = ZStream.fromIterable("0000000000000001".getBytes).via(ZHexPipeline.decode)
      for {
        value <- stream.run(ZDataSink.readLong)
      } yield assertTrue(value == 1L)
    },
    test("readIntLength works for 1") {
      val stream = ZStream.fromIterable("00000001".getBytes).via(ZHexPipeline.decode)
      for {
        value <- stream.run(ZDataSink.readIntLength)
      } yield assertTrue(value == 1)
    },
    test("readIntLength fails for -1") {
      val stream = ZStream.fromIterable("ffffffff".getBytes).via(ZHexPipeline.decode)
      for {
        exit <- stream.run(ZDataSink.readIntLength).exit
      } yield assert(exit)(fails(equalTo(NegativeLengthException(-1))))
    },
    test("readChunk works for zero-length") {
      val stream = ZStream.fromIterable("00000000".getBytes).via(ZHexPipeline.decode)
      for {
        value <- stream.run(ZDataSink.readIntLengthChunk)
      } yield assertTrue(value.isEmpty)
    },
    test("readChunk works for 5 specific Bytes") {
      val stream   = ZStream.fromIterable("0000000500017f80ff".getBytes).via(ZHexPipeline.decode)
      val expected = Chunk[Byte](0.toByte, 1.toByte, 127.toByte, -128.toByte, -1.toByte)
      for {
        value <- stream.run(ZDataSink.readIntLengthChunk)
      } yield assertTrue(value == expected)
    },
    test("readModifiedUTF8 works for Bytes DataOutputStream produces for String from hex Bytes \"e9a68a\"") {
      val expected = new String(stringForHex("e9a68a"))
      checkValueVsDOS[String](
        expected,
        (dos, expected) => dos.writeUTF(expected),
        ZDataSink.readModifiedUTF8
      )
    },
    test("readModifiedUTF8 works for Bytes DataOutputStream produces for \"馊\"") {
      val stream   = ZStream.fromIterable("0003e9a68a".getBytes).via(ZHexPipeline.decode)
      val expected = "馊"
      for {
        value <- stream.run(ZDataSink.readModifiedUTF8)
      } yield assertTrue(value == expected)
    },
    test("readModifiedUTF8 works for Bytes DataOutputStream produces for \"ܘ馊\"") {
      val expected = "ܘ馊"
      checkValueVsDOS[String](
        expected,
        (dos, expected) => dos.writeUTF(expected),
        ZDataSink.readModifiedUTF8
      )
    },
    test("Property test readBoolean") {
      checkVsDOS[Boolean](Gen.boolean, (dos, expected) => dos.writeBoolean(expected), ZDataSink.readBoolean)
    },
    test("Property test readByte") {
      checkVsDOS[Byte](Gen.byte, (dos, expected) => dos.writeByte(expected), ZDataSink.readByte)
    },
    test("Property test readChar") {
      checkVsDOS[Char](Gen.char, (dos, expected) => dos.writeChar(expected), ZDataSink.readChar)
    },
    test("Property test readDouble") {
      checkVsDOS[Double](Gen.double, (dos, expected) => dos.writeDouble(expected), ZDataSink.readDouble)
    },
    test("Property test readFloat") {
      checkVsDOS[Float](Gen.float, (dos, expected) => dos.writeFloat(expected), ZDataSink.readFloat)
    },
    test("Property test readInt") {
      checkVsDOS[Int](Gen.int, (dos, expected) => dos.writeInt(expected), ZDataSink.readInt)
    },
    test("Property test readLong") {
      checkVsDOS[Long](Gen.long, (dos, expected) => dos.writeLong(expected), ZDataSink.readLong)
    },
    test("Property test readShort") {
      checkVsDOS[Short](Gen.short, (dos, expected) => dos.writeShort(expected), ZDataSink.readShort)
    },
    test("Property test readModifiedUTF8 (1 char)") {
      checkVsDOS[String](
        Gen.stringBounded(0, 1)(Gen.unicodeChar),
        (dos, expected) => {
          dos.writeUTF(expected)
        },
        ZDataSink.readModifiedUTF8
      )
    },
    test("Property test readModifiedUTF8 (multiple chars)") {
      checkVsDOS[String](
        Gen.stringBounded(0, 100)(Gen.unicodeChar),
        (dos, expected) => {
          dos.writeUTF(expected)
        },
        ZDataSink.readModifiedUTF8
      )
    }
  )

}
