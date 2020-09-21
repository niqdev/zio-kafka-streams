package com.github.niqdev
package schema

import io.estatico.newtype.macros.newtype
import kafka.streams.serde.{ AvroCodec, Codec }

object dummy {

  // example Refined + newtype key
  @newtype case class DummyRefKey(string: UuidString)
  object DummyRefKey {
    implicit val eventIdCodec: Codec[DummyRefKey] =
      Codec.stringCodec.cmap[DummyRefKey](_.string.value, UuidString.from(_).toOption.map(DummyRefKey.apply))
  }

  final case class DummyKey(uuid: java.util.UUID)
  object DummyKey {
    final implicit val dummyKeyAvroCodec: AvroCodec[DummyKey] =
      AvroCodec.genericKey[DummyKey]
  }
  final case class DummyValue(value: String)
  object DummyValue {
    final implicit val dummyValueAvroCodec: AvroCodec[DummyValue] =
      AvroCodec.genericValue[DummyValue]
  }
}
