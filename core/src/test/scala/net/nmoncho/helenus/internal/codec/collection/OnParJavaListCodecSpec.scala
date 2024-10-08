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

import java.util

import scala.collection.Factory
import scala.collection.mutable as mutablecoll

import com.datastax.oss.driver.api.core.`type`.codec.TypeCodec
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import net.nmoncho.helenus.api.`type`.codec.Codec

abstract class OnParJavaListCodecSpec[ScalaColl[_] <: scala.collection.Seq[?]](name: String)(
    implicit factory: Factory[String, ScalaColl[String]]
) extends OnParJavaCodecSpec[ScalaColl, java.util.List](name):

    override val javaCodec: TypeCodec[java.util.List[String]] = TypeCodecs.listOf(TypeCodecs.TEXT)

end OnParJavaListCodecSpec

class OnParBufferCodecSpec extends OnParJavaListCodecSpec[mutablecoll.Buffer]("BufferCodec"):

    import scala.jdk.CollectionConverters.*

    override protected val codec: Codec[mutablecoll.Buffer[String]] =
        Codec[mutablecoll.Buffer[String]]

    override def toJava(t: mutablecoll.Buffer[String]): util.List[String] =
        if t == null then null else t.asJava
end OnParBufferCodecSpec

class OnParIndexedSeqCodecSpec
    extends OnParJavaListCodecSpec[mutablecoll.IndexedSeq]("IndexedSeqCodec"):

    import scala.jdk.CollectionConverters.*

    override protected val codec: Codec[mutablecoll.IndexedSeq[String]] =
        Codec[mutablecoll.IndexedSeq[String]]

    override def toJava(t: mutablecoll.IndexedSeq[String]): util.List[String] =
        if t == null then null else t.asJava
end OnParIndexedSeqCodecSpec

class OnParListCodecSpec extends OnParJavaListCodecSpec[List]("ListCodec"):

    import scala.jdk.CollectionConverters.*

    override protected val codec: Codec[List[String]] = Codec[List[String]]

    override def toJava(t: List[String]): util.List[String] = if t == null then null else t.asJava
end OnParListCodecSpec

class OnParSeqCodecSpec extends OnParJavaListCodecSpec[Seq]("SeqCodec"):

    import scala.jdk.CollectionConverters.*

    override protected val codec: Codec[Seq[String]] = Codec[Seq[String]]

    override def toJava(t: Seq[String]): util.List[String] = if t == null then null else t.asJava
end OnParSeqCodecSpec

class OnParVectorCodecSpec extends OnParJavaListCodecSpec[Vector]("VectorCodec"):

    import scala.jdk.CollectionConverters.*

    override protected val codec: Codec[Vector[String]] = Codec[Vector[String]]

    override def toJava(t: Vector[String]): util.List[String] = if t == null then null else t.asJava
end OnParVectorCodecSpec
