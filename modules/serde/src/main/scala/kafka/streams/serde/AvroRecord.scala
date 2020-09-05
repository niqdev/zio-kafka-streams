package kafka.streams.serde

import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.scala.kstream._

/**
  * See org.apache.kafka.streams.kstream.Consumed
  */
trait AvroRecordConsumed[K, V] {
  def consumed(schemaRegistry: String): Consumed[K, V]
}

object AvroRecordConsumed {
  def apply[K, V](implicit ev: AvroRecordConsumed[K, V]): AvroRecordConsumed[K, V] = ev

  implicit def avroRecordConsumed[K, V](
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): AvroRecordConsumed[K, V] =
    schemaRegistry =>
      Consumed.`with`[K, V](
        K.serde(schemaRegistry),
        V.serde(schemaRegistry)
      )
}

/**
  * See org.apache.kafka.streams.kstream.Grouped
  */
trait AvroRecordGrouped[K, V] {
  def grouped(schemaRegistry: String): Grouped[K, V]
}

object AvroRecordGrouped {
  def apply[K, V](implicit ev: AvroRecordGrouped[K, V]): AvroRecordGrouped[K, V] = ev

  implicit def avroRecordGrouped[K, V](
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): AvroRecordGrouped[K, V] =
    schemaRegistry =>
      Grouped.`with`[K, V](
        K.serde(schemaRegistry),
        V.serde(schemaRegistry)
      )
}

/**
  * See org.apache.kafka.streams.kstream.Joined
  */
trait AvroRecordJoined[K, V, VO] {
  def joined(schemaRegistry: String): Joined[K, V, VO]
}

object AvroRecordJoined {
  def apply[K, V, VO](implicit ev: AvroRecordJoined[K, V, VO]): AvroRecordJoined[K, V, VO] = ev

  implicit def avroRecordJoined[K, V, VO](
    implicit K: AvroCodec[K],
    V: AvroCodec[V],
    VO: AvroCodec[VO]
  ): AvroRecordJoined[K, V, VO] =
    schemaRegistry =>
      Joined.`with`[K, V, VO](
        K.serde(schemaRegistry),
        V.serde(schemaRegistry),
        VO.serde(schemaRegistry)
      )
}

/**
  * See org.apache.kafka.streams.kstream.Materialized
  */
trait AvroRecordMaterialized[K, V, S <: StateStore] {
  def materialize(schemaRegistry: String): Materialized[K, V, S]
}

object AvroRecordMaterialized {
  def apply[K, V, S <: StateStore](
    implicit ev: AvroRecordMaterialized[K, V, S]
  ): AvroRecordMaterialized[K, V, S] = ev

  implicit def avroRecordMaterialized[K, V, S <: StateStore](
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): AvroRecordMaterialized[K, V, S] =
    schemaRegistry =>
      Materialized.`with`[K, V, S](
        K.serde(schemaRegistry),
        V.serde(schemaRegistry)
      )
}

/**
  * See org.apache.kafka.streams.kstream.Produced
  */
trait AvroRecordProduced[K, V] {
  def produced(schemaRegistry: String): Produced[K, V]
}

object AvroRecordProduced {
  def apply[K, V](implicit ev: AvroRecordProduced[K, V]): AvroRecordProduced[K, V] = ev

  implicit def avroRecordProduced[K, V](
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): AvroRecordProduced[K, V] =
    schemaRegistry =>
      Produced.`with`[K, V](
        K.serde(schemaRegistry),
        V.serde(schemaRegistry)
      )
}

/**
  * See org.apache.kafka.streams.kstream.StreamJoined
  */
trait AvroRecordStreamJoined[K, V, VO] {
  def streamJoined(schemaRegistry: String): StreamJoined[K, V, VO]
}

object AvroRecordStreamJoined {
  def apply[K, V, VO](implicit ev: AvroRecordStreamJoined[K, V, VO]): AvroRecordStreamJoined[K, V, VO] = ev

  implicit def avroRecordStreamJoined[K, V, VO](
    implicit K: AvroCodec[K],
    V: AvroCodec[V],
    VO: AvroCodec[VO]
  ): AvroRecordStreamJoined[K, V, VO] =
    schemaRegistry =>
      StreamJoined.`with`[K, V, VO](
        K.serde(schemaRegistry),
        V.serde(schemaRegistry),
        VO.serde(schemaRegistry)
      )
}
