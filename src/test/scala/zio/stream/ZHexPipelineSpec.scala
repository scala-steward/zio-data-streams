package zio.stream

import zio.*
import zio.stream.*
import zio.stream.ZHexPipeline.HexDecodeException
import zio.test.*

object ZHexPipelineSpec extends ZIOSpecDefault {

  def testDecode(s: String, b: Byte): ZIO[Any, HexDecodeException, TestResult] =
    ZStream.fromIterable(s.getBytes).via(ZHexPipeline.decode).run(ZSink.head).map { v => assertTrue(v.get == b) }

  def testEncode(b: Byte, s: String): ZIO[Any, Nothing, TestResult] =
    ZStream(b).via(ZHexPipeline.encode).run(ZSink.collectAll).map { v =>
      val t = new String(v.toArray)
      assertTrue(t == s)
    }

  def spec: Spec[Any, HexDecodeException] = suite("ZHexPipelineSpec")(
    test("Hex for Byte 0 is 0x00") {
      testEncode(0.toByte, "00")
    },
    test("Hex for Byte 1 is 0x01") {
      testEncode(1.toByte, "01")
    },
    test("Hex for Byte 127 is 0x7f") {
      testEncode(127.toByte, "7f")
    },
    test("Hex for Byte -128 is 0x80") {
      testEncode(-128.toByte, "80")
    },
    test("Hex for Byte -1 is 0xff") {
      testEncode(-1.toByte, "ff")
    },
    test("Byte for hex 0x00 is 0") {
      testDecode("00", 0.toByte)
    },
    test("Byte for hex 0x01 is 1") {
      testDecode("01", 1.toByte)
    },
    test("Byte for hex 0x7f is 127") {
      testDecode("7f", 127.toByte)
    },
    test("Byte for hex 0x80 is -128") {
      testDecode("80", -128.toByte)
    },
    test("Byte for hex 0xff is -1") {
      testDecode("ff", -1.toByte)
    }
  )

}
