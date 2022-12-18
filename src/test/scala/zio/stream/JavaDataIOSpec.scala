package zio.stream

import zio.test.*
import zio.test.Assertion.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

object JavaDataIOSpec extends ZIOSpecDefault {

  def spec = suite("JavaDataIOSpec")(
    test("Property test DataInputStream vs DataOutputStream for Modified UTF8 (1 char)") {
      val gen = Gen.stringBounded(0, 1)(Gen.unicodeChar)
      check(gen) { expected =>
        val bos = new ByteArrayOutputStream
        val dos = new DataOutputStream(bos)
        dos.writeUTF(expected)
        dos.flush()
        val bytes = bos.toByteArray
        val dis = new DataInputStream(new ByteArrayInputStream(bytes))
        val v = dis.readUTF()
        assertTrue(v == expected)
      }
    },
    test("Property test DataInputStream vs DataOutputStream for Modified UTF8 (multiple chars)") {
      val gen = Gen.stringBounded(0, 100)(Gen.unicodeChar)
      check(gen) { expected =>
        val bos = new ByteArrayOutputStream
        val dos = new DataOutputStream(bos)
        dos.writeUTF(expected)
        dos.flush()
        val bytes = bos.toByteArray
        val dis = new DataInputStream(new ByteArrayInputStream(bytes))
        val v = dis.readUTF()
        assertTrue(v == expected)
      }
    }
  )

}
