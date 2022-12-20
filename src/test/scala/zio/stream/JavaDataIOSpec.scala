/*
 * Copyright 2018-2022 Gregor Purdy
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
