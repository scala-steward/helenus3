/*
 * Copyright 2021 the original author or authors
 *
 * SPDX-License-Identifier: MIT
 */

package net.nmoncho.helenus.internal.codec.collection
package mutable

import scala.collection.mutable as mutablecoll

import com.datastax.oss.driver.api.core.`type`.codec.TypeCodec
import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.shaded.guava.common.reflect.ScalaTypeToken
import com.datastax.oss.driver.shaded.guava.common.reflect.TypeParameter

class BufferCodec[T](inner: TypeCodec[T], frozen: Boolean)
    extends AbstractSeqCodec[T, mutablecoll.Buffer](inner, frozen):

    override val getJavaType: GenericType[mutablecoll.Buffer[T]] =
        GenericType
            .of(new ScalaTypeToken[mutablecoll.Buffer[T]] {}
                .where(new TypeParameter[T] {}, inner.getJavaType().getType())
                .getType())
            .asInstanceOf[GenericType[mutablecoll.Buffer[T]]]

    override def toString: String = s"ArrayBufferCodec[${inner.getCqlType.toString}]"
end BufferCodec

object BufferCodec:
    def apply[T](inner: TypeCodec[T], frozen: Boolean): BufferCodec[T] =
        new BufferCodec(inner, frozen)

    def frozen[T](inner: TypeCodec[T]): BufferCodec[T] =
        BufferCodec[T](inner, frozen = true)
end BufferCodec

class IndexedSeqCodec[T](inner: TypeCodec[T], frozen: Boolean)
    extends AbstractSeqCodec[T, mutablecoll.IndexedSeq](inner, frozen):

    override val getJavaType: GenericType[mutablecoll.IndexedSeq[T]] =
        GenericType
            .of(new ScalaTypeToken[mutablecoll.IndexedSeq[T]] {}
                .where(new TypeParameter[T] {}, inner.getJavaType().getType())
                .getType())
            .asInstanceOf[GenericType[mutablecoll.IndexedSeq[T]]]

    override def toString: String = s"IndexedSeqCodec[${inner.getCqlType.toString}]"
end IndexedSeqCodec

object IndexedSeqCodec:
    def apply[T](inner: TypeCodec[T], frozen: Boolean): IndexedSeqCodec[T] =
        new IndexedSeqCodec(inner, frozen)

    def frozen[T](inner: TypeCodec[T]): IndexedSeqCodec[T] =
        IndexedSeqCodec[T](inner, frozen = true)
end IndexedSeqCodec

class SetCodec[T](inner: TypeCodec[T], frozen: Boolean)
    extends AbstractSetCodec[T, mutablecoll.Set](inner, frozen):

    override val getJavaType: GenericType[mutablecoll.Set[T]] =
        GenericType
            .of(new ScalaTypeToken[mutablecoll.Set[T]] {}
                .where(new TypeParameter[T] {}, inner.getJavaType().getType())
                .getType())
            .asInstanceOf[GenericType[mutablecoll.Set[T]]]

    override def toString: String = s"MutableSetCodec[${inner.getCqlType.toString}]"
end SetCodec

object SetCodec:
    def apply[T](inner: TypeCodec[T], frozen: Boolean): SetCodec[T] =
        new SetCodec(inner, frozen)

    def frozen[T](inner: TypeCodec[T]): SetCodec[T] =
        SetCodec[T](inner, frozen = true)
end SetCodec

class MapCodec[K, V](keyInner: TypeCodec[K], valueInner: TypeCodec[V], frozen: Boolean)
    extends AbstractMapCodec[K, V, mutablecoll.Map](keyInner, valueInner, frozen):

    override val getJavaType: GenericType[mutablecoll.Map[K, V]] =
        GenericType
            .of(new ScalaTypeToken[mutablecoll.Map[K, V]] {}
                .where(new TypeParameter[K] {}, keyInner.getJavaType().getType())
                .where(new TypeParameter[V] {}, valueInner.getJavaType().getType())
                .getType())
            .asInstanceOf[GenericType[mutablecoll.Map[K, V]]]

    override def toString: String =
        s"MutableMapCodec[${keyInner.getCqlType.toString},${valueInner.getCqlType.toString}]"
end MapCodec

object MapCodec:
    def apply[K, V](
        keyInner: TypeCodec[K],
        valueInner: TypeCodec[V],
        frozen: Boolean
    ): MapCodec[K, V] =
        new MapCodec(keyInner, valueInner, frozen)

    def frozen[K, V](keyInner: TypeCodec[K], valueInner: TypeCodec[V]): MapCodec[K, V] =
        MapCodec[K, V](keyInner, valueInner, frozen = true)
end MapCodec
