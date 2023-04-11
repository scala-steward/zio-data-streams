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

import zio.*
import zio.stream.*
import zio.test.*
import zio.test.Assertion.{fails, equalTo}

import java.nio.charset.StandardCharsets

object ZHexPipelineSpec extends ZIOSpecDefault {

  def testHexDecode(s: String, b: Byte): ZIO[Any, EncodingException, TestResult] =
    ZStream
      .fromIterable(s.getBytes(StandardCharsets.UTF_8))
      .via(ZHexPipeline.hexDecode)
      .run(ZSink.head)
      .map(v => assertTrue(v.get == b))

  def testHexEncode(b: Byte, s: String): ZIO[Any, Nothing, TestResult] =
    ZStream(b).via(ZHexPipeline.hexEncode).run(ZSink.collectAll).map { v =>
      val t = new String(v.toArray)
      assertTrue(t == s)
    }

  def spec: Spec[Any, EncodingException] = suite("ZHexPipelineSpec")(
    test("Empty input encodes to empty output") {
      for {
        result <- ZStream.empty.via(ZHexPipeline.hexEncode).run(ZSink.collectAll[Byte])
      } yield assert(result)(equalTo(Chunk.empty[Byte]))
    },
    test("Hex for Byte 0 is 0x00") {
      testHexEncode(0.toByte, "00")
    },
    test("Hex for Byte 1 is 0x01") {
      testHexEncode(1.toByte, "01")
    },
    test("Hex for Byte 127 is 0x7f") {
      testHexEncode(127.toByte, "7f")
    },
    test("Hex for Byte -128 is 0x80") {
      testHexEncode(-128.toByte, "80")
    },
    test("Hex for Byte -1 is 0xff") {
      testHexEncode(-1.toByte, "ff")
    },
    test("Byte for hex 0x00 is 0") {
      testHexDecode("00", 0.toByte)
    },
    test("Byte for hex 0x01 is 1") {
      testHexDecode("01", 1.toByte)
    },
    test("Byte for hex 0x7f is 127") {
      testHexDecode("7f", 127.toByte)
    },
    test("Byte for hex 0x80 is -128") {
      testHexDecode("80", -128.toByte)
    },
    test("Byte for hex 0xff is -1") {
      testHexDecode("ff", -1.toByte)
    },
    test("Empty input decodes to empty output") {
      for {
        result <- ZStream.empty.via(ZHexPipeline.hexDecode).run(ZSink.collectAll[Byte])
      } yield assert(result)(equalTo(Chunk.empty[Byte]))
    },
    test("Odd number of hex digits causes error on decode") {
      for {
        result <-
          ZStream
            .fromIterable("abc".getBytes(StandardCharsets.UTF_8))
            .via(ZHexPipeline.hexDecode)
            .run(ZSink.collectAll[Byte])
            .exit
      } yield assert(result)(fails(equalTo(EncodingException("Extra input at end after last fully encoded byte"))))
    },
    test("Non hex digit causes error on decode") {
      for {
        result <-
          ZStream
            .fromIterable("ag".getBytes(StandardCharsets.UTF_8))
            .via(ZHexPipeline.hexDecode)
            .run(ZSink.collectAll[Byte])
            .exit
      } yield assert(result)(fails(equalTo(EncodingException("Not a valid hex digit: 'g'"))))
    }
  )

}
