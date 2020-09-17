package com.github.niqdev
package schema

import kafka.streams.serde.AvroCodec

object dummy {
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
