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

import zio._
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.test._

import java.io.DataInputStream

object ZDataStreamSpec extends ZIOSpecDefault {

  def doTest(stream: => ZStream[Any, Nothing, Byte], string: String): ZIO[Any, Nothing, TestResult] = for {
    value <- stream
               .via(ZPipeline.hexEncode)
               .run(ZSink.collectAll[Char])
               .map(cs => new String(cs.toArray))
  } yield assertTrue(value == string)

  private def checkVsDIS[A](gen: Gen[Any, A], g: A => ZStream[Any, Throwable, Byte], f: DataInputStream => A) =
    check(gen) { expected =>
      for {
        value <- ZIO.scoped {
                   for {
                     _     <- ZIO.unit
                     stream = g(expected)
                     is    <- stream.toInputStream
                     dis    = new DataInputStream(is)
                   } yield f(dis)
                 }
      } yield assertTrue(value == expected)
    }

  def spec = suite("ZDataStreamSpec")(
    test("writeBoolean works for true") {
      doTest(ZDataStream.writeBoolean(true), "01")
    },
    test("writeChar works for 1") {
      doTest(ZDataStream.writeChar(1.toChar), "0001")
    },
    test("writeInt works for 1") {
      doTest(ZDataStream.writeInt(1), "00000001")
    },
    test("writeLong works for 1") {
      doTest(ZDataStream.writeLong(1L), "0000000000000001")
    },
    test("writeShort works for 1") {
      doTest(ZDataStream.writeShort(1.toShort), "0001")
    },
    test("Property test writeBoolean") {
      checkVsDIS(Gen.boolean, ZDataStream.writeBoolean, dis => dis.readBoolean)
    },
    test("Property test writeChar") {
      checkVsDIS(Gen.char, ZDataStream.writeChar, dis => dis.readChar)
    },
    test("Property test writeDouble") {
      checkVsDIS(Gen.double, ZDataStream.writeDouble, dis => dis.readDouble)
    },
    test("Property test writeFloat") {
      checkVsDIS(Gen.float, ZDataStream.writeFloat, dis => dis.readFloat)
    },
    test("Property test writeInt") {
      checkVsDIS(Gen.int, ZDataStream.writeInt, dis => dis.readInt)
    },
    test("Property test writeLong") {
      checkVsDIS(Gen.long, ZDataStream.writeLong, dis => dis.readLong)
    },
    test("Property test writeModifiedUTF8 (1 char)") {
      checkVsDIS(Gen.stringBounded(0, 1)(Gen.unicodeChar), ZDataStream.writeModifiedUTF8, dis => dis.readUTF)
    },
    test("Property test writeModifiedUTF8 (multiple chars)") {
      checkVsDIS(Gen.stringBounded(0, 100)(Gen.unicodeChar), ZDataStream.writeModifiedUTF8, dis => dis.readUTF)
    },
    test("Property test writeShort") {
      checkVsDIS(Gen.short, ZDataStream.writeShort, dis => dis.readShort)
    }
  )

}
