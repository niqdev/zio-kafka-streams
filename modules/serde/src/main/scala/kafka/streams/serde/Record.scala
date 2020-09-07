package kafka.streams.serde

import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.scala.kstream._

/**
  * See org.apache.kafka.streams.kstream.Consumed
  */
trait RecordConsumed[K, V] {
  def consumed: Consumed[K, V]
}

object RecordConsumed {
  def apply[K, V](implicit ev: RecordConsumed[K, V]): RecordConsumed[K, V] = ev

  implicit def recordConsumed[K, V](
    implicit K: Codec[K],
    V: Codec[V]
  ): RecordConsumed[K, V] =
    new RecordConsumed[K, V] {
      override def consumed: Consumed[K, V] =
        Consumed.`with`[K, V](K.serde, V.serde)
    }
}

/**
  * See org.apache.kafka.streams.kstream.Grouped
  */
trait RecordGrouped[K, V] {
  def grouped: Grouped[K, V]
}

object RecordGrouped {
  def apply[K, V](implicit ev: RecordGrouped[K, V]): RecordGrouped[K, V] = ev

  implicit def recordGrouped[K, V](
    implicit K: Codec[K],
    V: Codec[V]
  ): RecordGrouped[K, V] =
    new RecordGrouped[K, V] {
      override def grouped: Grouped[K, V] =
        Grouped.`with`[K, V](K.serde, V.serde)
    }
}

/**
  * See org.apache.kafka.streams.kstream.Joined
  */
trait RecordJoined[K, V, VO] {
  def joined: Joined[K, V, VO]
}

object RecordJoined {
  def apply[K, V, VO](implicit ev: RecordJoined[K, V, VO]): RecordJoined[K, V, VO] = ev

  implicit def recordJoined[K, V, VO](
    implicit K: Codec[K],
    V: Codec[V],
    VO: Codec[VO]
  ): RecordJoined[K, V, VO] =
    new RecordJoined[K, V, VO] {
      override def joined: Joined[K, V, VO] =
        Joined.`with`[K, V, VO](K.serde, V.serde, VO.serde)
    }
}

/**
  * See org.apache.kafka.streams.kstream.Materialized
  */
trait RecordMaterialized[K, V, S <: StateStore] {
  def materialize: Materialized[K, V, S]
}

object RecordMaterialized {
  def apply[K, V, S <: StateStore](
    implicit ev: RecordMaterialized[K, V, S]
  ): RecordMaterialized[K, V, S] = ev

  implicit def recordMaterialized[K, V, S <: StateStore](
    implicit K: Codec[K],
    V: Codec[V]
  ): RecordMaterialized[K, V, S] =
    new RecordMaterialized[K, V, S] {
      override def materialize: Materialized[K, V, S] =
        Materialized.`with`[K, V, S](K.serde, V.serde)
    }
}

/**
  * See org.apache.kafka.streams.kstream.Produced
  */
trait RecordProduced[K, V] {
  def produced: Produced[K, V]
}

object RecordProduced {
  def apply[K, V](implicit ev: RecordProduced[K, V]): RecordProduced[K, V] = ev

  implicit def recordProduced[K, V](
    implicit K: Codec[K],
    V: Codec[V]
  ): RecordProduced[K, V] =
    new RecordProduced[K, V] {
      override def produced: Produced[K, V] =
        Produced.`with`[K, V](K.serde, V.serde)
    }
}

/**
  * See org.apache.kafka.streams.kstream.StreamJoined
  */
trait RecordStreamJoined[K, V, VO] {
  def streamJoined: StreamJoined[K, V, VO]
}

object RecordStreamJoined {
  def apply[K, V, VO](implicit ev: RecordStreamJoined[K, V, VO]): RecordStreamJoined[K, V, VO] = ev

  implicit def recordStreamJoined[K, V, VO](
    implicit K: Codec[K],
    V: Codec[V],
    VO: Codec[VO]
  ): RecordStreamJoined[K, V, VO] =
    new RecordStreamJoined[K, V, VO] {
      override def streamJoined: StreamJoined[K, V, VO] =
        StreamJoined.`with`[K, V, VO](K.serde, V.serde, VO.serde)
    }
}
