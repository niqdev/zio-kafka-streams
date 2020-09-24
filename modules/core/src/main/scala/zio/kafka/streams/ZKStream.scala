package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import zio._

// TODO incomplete + tests + docs
// TODO ZKGroupedStream
sealed abstract class ZKStream[K, V](private val stream: KStream[K, V]) {

  def map[KO, VO](f: (K, V) => (KO, VO)): Task[ZKStream[KO, VO]] =
    ZKStream(stream.map(f))

  def mapKey[KO](f: K => KO): Task[ZKStream[KO, V]] =
    ZKStream(stream.map((key, value) => f(key) -> value))

  def mapValue[VO](f: V => VO): Task[ZKStream[K, VO]] =
    ZKStream(stream.mapValues(f))

  // TODO foreach async
  def tap(a: (K, V) => Task[Unit]): Task[ZKStream[K, V]] = ???

  def filter(p: (K, V) => Boolean): Task[ZKStream[K, V]] =
    ZKStream(stream.filter(p))

  def filterNot(p: (K, V) => Boolean): Task[ZKStream[K, V]] =
    ZKStream(stream.filterNot(p))

  def span(p: (K, V) => Boolean): Task[(ZKStream[K, V], ZKStream[K, V])] =
    Task.effect(stream.branch(p)).map { branches =>
      (ZKStream.newInstance(branches(0)), ZKStream.newInstance(branches(1)))
    }

  def toProduced(topic: String): Produced[K, V] => RIO[KafkaStreamsConfig, Unit] =
    produced =>
      for {
        config <- KafkaStreamsConfig.config
        _ <- Task.effect {
          if (config.debug) stream.print(Printed.toSysOut[K, V].withLabel(topic))
          stream.to(topic)(produced)
        }
      } yield ()

  def to(topic: String)(
    implicit P: RecordProduced[K, V]
  ): RIO[KafkaStreamsConfig, Unit] =
    toProduced(topic)(P.produced)

  def toAvro(topic: String)(
    implicit P: AvroRecordProduced[K, V]
  ): RIO[KafkaStreamsConfig, Unit] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl => toProduced(topic)(P.produced(schemaRegistryUrl)))

  def toTable(
    implicit M: RecordMaterialized[K, V, ByteArrayKeyValueStore]
  ): RIO[KafkaStreamsConfig, ZKTable[K, V]] =
    ZKTable(stream.toTable(M.materialize))

  def toTableAvro(
    implicit M: AvroRecordMaterialized[K, V, ByteArrayKeyValueStore]
  ): RIO[KafkaStreamsConfig, ZKTable[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl => ZKTable(stream.toTable(M.materialize(schemaRegistryUrl))))
}

object ZKStream {

  def newInstance[K, V](stream: KStream[K, V]): ZKStream[K, V] =
    new ZKStream[K, V](stream) {}

  def apply[K, V](stream: KStream[K, V]): Task[ZKStream[K, V]] =
    Task.effect(newInstance(stream))
}
