/*
 * Copyright (c) 2021 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.nmoncho.helenus
package internal.codec.collection

import scala.collection.Factory
import scala.collection.mutable as mutablecoll

import com.datastax.oss.driver.api.core.ProtocolVersion
import net.nmoncho.helenus.api.`type`.codec.Codec
import net.nmoncho.helenus.internal.codec.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

abstract class AbstractSetCodecSpec[Coll[_] <: scala.collection.Set[?]](name: String)(
    implicit intFactory: Factory[Int, Coll[Int]],
    stringFactory: Factory[String, Coll[String]]
) extends AnyWordSpec
    with Matchers
    with CodecSpecBase[Coll[Int]]:

    override protected val codec: Codec[Coll[Int]]
    protected val sCodec: Codec[Coll[String]]

    private val emptySet = intFactory.newBuilder.result()
    private val oneTwoThree =
        val builder = intFactory.newBuilder
        builder ++= Seq(1, 2, 3)
        builder.result()

    name should {
        "encode" in {
            encode(null.asInstanceOf[Coll[Int]]) shouldBe None
            encode(emptySet) shouldBe Some("0x00000000")
            encode(oneTwoThree) shouldBe Some(
              "0x00000003000000040000000100000004000000020000000400000003"
            )
        }

        "decode" in {
            decode(null) shouldBe Some(emptySet)
            decode("0x00000000") shouldBe Some(emptySet)
            decode("0x00000003000000040000000100000004000000020000000400000003") shouldBe Some(
              oneTwoThree
            )
        }

        "fail to encode" in {
            val oneNullThree =
                val builder = stringFactory.newBuilder
                builder ++= Seq("1", null, "3")
                builder.result()

            intercept[IllegalArgumentException](
              sCodec.encode(oneNullThree, ProtocolVersion.DEFAULT)
            )
        }

        "format" in {
            format(null.asInstanceOf[Coll[Int]]) shouldBe NULL
            format(emptySet) shouldBe "{}"
            format(oneTwoThree) shouldBe "{1,2,3}"
        }

        "parse" in {
            parse("") shouldBe null
            parse(NULL) shouldBe null
            parse(NULL.toLowerCase) shouldBe null
            parse("{}") shouldBe Set.empty[Int]
            parse("{1,2,3}") shouldBe Set(1, 2, 3)
            parse(" { 1 , 2 , 3 } ") shouldBe Set(1, 2, 3)
        }

        "fail to parse invalid input" in {
            val invalid = Seq(
              "1,2,3}",
              "{1,2,3",
              "{1 2,3}",
              "{{1,2,3}"
            )

            invalid.foreach { input =>
                intercept[IllegalArgumentException] {
                    parse(input)
                }
            }
        }

        "accept generic type" in {
            codec.accepts(codec.getJavaType) shouldBe true
        }

        "accept objects" in {
            val fooBar =
                val builder = stringFactory.newBuilder
                builder ++= Seq("foo", "bar")
                builder.result()

            codec.accepts(oneTwoThree) shouldBe true
            codec.accepts(fooBar) shouldBe false
        }
    }
end AbstractSetCodecSpec

class SetCodecSpec extends AbstractSetCodecSpec[Set]("SetCodec"):
    override protected val codec: Codec[Set[Int]]     = Codec[Set[Int]]
    override protected val sCodec: Codec[Set[String]] = Codec[Set[String]]

class MutableSetCodecSpec extends AbstractSetCodecSpec[mutablecoll.Set]("MutableSetCodec"):
    override protected val codec: Codec[mutablecoll.Set[Int]] = Codec[mutablecoll.Set[Int]]
    override protected val sCodec: Codec[mutablecoll.Set[String]] =
        Codec[mutablecoll.Set[String]]
