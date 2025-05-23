/*
 * Copyright 2021 the original author or authors
 *
 * SPDX-License-Identifier: MIT
 */

package net.nmoncho.helenus
package internal.codec

import java.util.Optional
import java.util.UUID

import com.datastax.oss.driver.api.core.`type`.DataTypes
import com.datastax.oss.driver.api.core.`type`.codec.*
import com.datastax.oss.driver.internal.core.`type`.codec.extras.OptionalCodec
import net.nmoncho.helenus.api.`type`.codec.Codec
import net.nmoncho.helenus.utils.CassandraSpec
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OptionCodecSpec
    extends AnyWordSpec
    with Matchers
    with CodecSpecBase[Option[Int]]
    with CassandraSpec:

    override protected val codec: Codec[Option[Int]] = Codec[Option[Int]]

    private val optionStringCodec = Codec[Option[String]]

    "OptionCodec" should {
        "encode" in {
            encode(Some(1)) shouldBe Some("0x00000001")
            encode(None) shouldBe None
            encode(null) shouldBe None
        }

        "decode" in {
            decode("0x00000001") shouldBe Some(Some(1))
            decode("0x") shouldBe Some(None)
            decode(null) shouldBe Some(None)
        }

        "format" in {
            format(Some(1)) shouldBe "1"
            format(None) shouldBe NULL
            format(null) shouldBe NULL
        }

        "parse" in {
            parse("1") shouldBe Some(1)
            parse(NULL) shouldBe None
            parse(NULL.toLowerCase()) shouldBe None
            parse("") shouldBe None
            parse(null) shouldBe None
        }

        "fail to parse invalid input" in {
            intercept[IllegalArgumentException] {
                parse("maybe")
            }
        }

        "accept generic type" in {
            val anotherIntCodec = Codec[Option[Int]]

            codec.accepts(codec.getJavaType) shouldBe true
            codec.accepts(anotherIntCodec.getJavaType) shouldBe true
            codec.accepts(optionStringCodec.getJavaType) shouldBe false
        }

        "raw type" in {
            codec.accepts(classOf[Option[?]]) shouldBe true
        }

        "accept objects" in {
            codec.accepts(Some(1)) shouldBe true
            codec.accepts(None) shouldBe true
            codec.accepts(Int.MaxValue) shouldBe false
            codec.accepts(Some(2.0)) shouldBe false
            codec.accepts(Some("foobar")) shouldBe false
        }

        "be queried correctly in the registry" in {
            val codecRegistry = session.getContext.getCodecRegistry

            codecRegistry.codecFor(codec.getJavaType) shouldBe codec
            codecRegistry.codecFor(optionStringCodec.getJavaType) shouldBe optionStringCodec

            codecRegistry.codecFor(DataTypes.INT, classOf[Option[?]]) shouldBe codec
            codecRegistry.codecFor(DataTypes.TEXT, classOf[Option[?]]) shouldBe optionStringCodec
            codecRegistry.codecFor(DataTypes.INT, None) shouldBe codec
            codecRegistry.codecFor(DataTypes.INT, Some(1)) shouldBe codec
            codecRegistry.codecFor(DataTypes.TEXT, None) shouldBe optionStringCodec
            codecRegistry.codecFor(DataTypes.TEXT, Some("foo")) shouldBe optionStringCodec

            intercept[CodecNotFoundException] {
                codecRegistry.codecFor(DataTypes.INT, Some("foo")) shouldBe optionStringCodec
            }
            intercept[CodecNotFoundException] {
                codecRegistry.codecFor(DataTypes.TEXT, Some(1)) shouldBe optionStringCodec
            }

            codecRegistry.codecFor(Some(1)) shouldBe codec
            codecRegistry.codecFor(Some("foo")) shouldBe optionStringCodec

            intercept[TestFailedException] {
                // This fails due to both codecs accepting the same value
                codecRegistry.codecFor(None) shouldBe codec
                codecRegistry.codecFor(None) shouldBe optionStringCodec
            }
        }

        "work with a table" in {
            withClue("when inserting nulls") {
                val id = UUID.randomUUID()
                session.execute(
                  s"INSERT INTO option_table(id, opt_int, opt_str) VALUES ($id, null, null)"
                )
                val rs = session.execute(s"SELECT * from option_table WHERE id = $id")

                rs.forEach { row =>
                    row.get(0, classOf[UUID]) shouldBe id

                    withClue("for class") {
                        row.get(1, classOf[Option[?]]) shouldBe None
                        row.get(2, classOf[Option[?]]) shouldBe None
                    }

                    withClue("for TypeCodec") {
                        row.get(1, codec) shouldBe None
                        row.get(2, optionStringCodec) shouldBe None
                    }

                    withClue("for GenericType") {
                        row.get(1, codec.getJavaType) shouldBe None
                        row.get(2, optionStringCodec.getJavaType) shouldBe None
                    }

                    withClue("fail when using the wrong codec") {
                        intercept[CodecNotFoundException] {
                            row.get(1, optionStringCodec.getJavaType) shouldBe None
                        }
                    }

                }
            }

            withClue("when inserting values") {
                val id = UUID.randomUUID()
                session.execute(
                  s"INSERT INTO option_table(id, opt_int, opt_str) VALUES ($id, 42, 'foo')"
                )
                val rs = session.execute(s"SELECT * from option_table WHERE id = $id")

                rs.forEach { row =>
                    row.get(0, classOf[UUID]) shouldBe id
                    val expectedInt    = Some(42)
                    val expectedString = Some("foo")

                    withClue("for class") {
                        row.get(1, classOf[Option[?]]) shouldBe expectedInt
                        row.get(2, classOf[Option[?]]) shouldBe expectedString
                    }

                    withClue("for TypeCodec") {
                        row.get(1, codec) shouldBe expectedInt
                        row.get(2, optionStringCodec) shouldBe expectedString
                    }

                    withClue("for GenericType") {
                        row.get(1, codec.getJavaType) shouldBe expectedInt
                        row.get(2, optionStringCodec.getJavaType) shouldBe expectedString
                    }

                    withClue("fail when using the wrong codec") {
                        intercept[CodecNotFoundException] {
                            row.get(1, optionStringCodec.getJavaType) shouldBe expectedInt
                        }
                    }
                }
            }
        }
    }

    override def beforeAll(): Unit =
        super.beforeAll()
        executeDDL("""CREATE TABLE IF NOT EXISTS option_table(
        |   id         UUID,
        |   opt_int    INT,
        |   opt_str    TEXT,
        |   PRIMARY KEY (id)
        |)""".stripMargin)
        registerCodec(codec)
        registerCodec(optionStringCodec)
    end beforeAll

end OptionCodecSpec

class OnParOptionCodecSpec
    extends AnyWordSpec
    with Matchers
    with CodecSpecBase[Option[String]]
    with OnParCodecSpec[Option[String], java.util.Optional[String]]:

    "OptionCodec" should {
        "on par with Java Codec (encode-decode)" in testEncodeDecode(
          null,
          None,
          Some("foo")
        )

        "on par with Java Codec (parse-format)" in testParseFormat(
          null,
          None,
          Some("foo")
        )
    }

    override protected val codec: Codec[Option[String]] = Codec[Option[String]]

    override def javaCodec: TypeCodec[Optional[String]] = new OptionalCodec[String](TypeCodecs.TEXT)

    override def toJava(t: Option[String]): Optional[String] =
        if t == null then null
        else t.fold(Optional.empty[String]())(Optional.of)

end OnParOptionCodecSpec
