/*
 * Copyright 2021 the original author or authors
 *
 * SPDX-License-Identifier: MIT
 */

package net.nmoncho.helenus
package internal.codec.enums

import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import net.nmoncho.helenus.api.`type`.codec.Codec
import net.nmoncho.helenus.internal.codec.CodecSpecBase
import net.nmoncho.helenus.internal.codec.enums.EnumNominalCodecSpec.Fingers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnumNominalCodecSpec extends AnyWordSpec with Matchers with CodecSpecBase[Fingers]:

    override protected val codec: Codec[Fingers] = Codec[Fingers]

    "EnumNominalCodecSpec" should {
        "encode" in {
            encode(Fingers.Ring) shouldBe Some("0x52696e67")
            encode(Fingers.Index) shouldBe Some("0x496e646578")
            encode(Fingers.Little) shouldBe Some("0x4c6974746c65")
        }

        "decode" in {
            decode("0x52696e67") shouldBe Some(Fingers.Ring)
            decode("0x496e646578") shouldBe Some(Fingers.Index)
            decode("0x4c6974746c65") shouldBe Some(Fingers.Little)
        }

        "fail to decode wrong value" in {
            intercept[NoSuchElementException] {
                decode("0x52696e6e")
            }
        }

        "format" in {
            format(Fingers.Ring) shouldBe quote("Ring")
            format(Fingers.Index) shouldBe quote("Index")
            format(null) shouldBe "NULL"
        }

        "parse" in {
            parse(quote("Ring")) shouldBe Fingers.Ring
            parse(quote("Index")) shouldBe Fingers.Index
            parse("null") shouldBe null
            parse("") shouldBe null
            parse(null) shouldBe null
        }

        "fail to parse invalid input" in {
            intercept[IllegalArgumentException] {
                parse("not a finger")
            }
        }

        "accept generic type" in {
            codec.accepts(GenericType.of(classOf[Fingers])) shouldBe true
            codec.accepts(GenericType.of(classOf[Float])) shouldBe false
        }

        "accept raw type" in {
            codec.accepts(classOf[Fingers]) shouldBe true
            codec.accepts(classOf[Float]) shouldBe false
        }

        "accept objects" in {
            codec.accepts(Fingers.Index) shouldBe true
            codec.accepts(Double.MaxValue) shouldBe false
        }
    }
end EnumNominalCodecSpec

object EnumNominalCodecSpec:

    enum Fingers derives NominalEnumCodec:
        case Thumb, Index, Middle, Ring, Little

end EnumNominalCodecSpec
