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

package net.nmoncho.helenus.internal.codec.collection

import scala.collection.immutable.SortedMap

import com.datastax.oss.driver.api.core.`type`.codec.TypeCodec
import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.api.core.`type`.reflect.GenericTypeParameter
import com.datastax.oss.driver.shaded.guava.common.reflect.ScalaTypeToken
import com.datastax.oss.driver.shaded.guava.common.reflect.TypeParameter
import com.datastax.oss.driver.shaded.guava.common.reflect.TypeToken

class SortedMapCodec[K: Ordering, V](
    keyInner: TypeCodec[K],
    valueInner: TypeCodec[V],
    frozen: Boolean
) extends AbstractMapCodec[K, V, SortedMap](keyInner, valueInner, frozen):

    override val getJavaType: GenericType[SortedMap[K, V]] =
        GenericType
            .of(new ScalaTypeToken[SortedMap[K, V]] {}
                .where(new TypeParameter[K] {}, keyInner.getJavaType().getType())
                .where(new TypeParameter[V] {}, valueInner.getJavaType().getType())
                .getType())
            .asInstanceOf[GenericType[SortedMap[K, V]]]

    override def toString: String =
        s"SortedMapCodec[${keyInner.getCqlType.toString}, ${valueInner.getCqlType.toString}]"
end SortedMapCodec

object SortedMapCodec:
    def apply[K: Ordering, V](
        keyInner: TypeCodec[K],
        valueInner: TypeCodec[V],
        frozen: Boolean
    ): SortedMapCodec[K, V] =
        new SortedMapCodec(keyInner, valueInner, frozen)

    def frozen[K: Ordering, V](
        keyInner: TypeCodec[K],
        valueInner: TypeCodec[V]
    ): SortedMapCodec[K, V] =
        SortedMapCodec(keyInner, valueInner, frozen = true)
end SortedMapCodec
