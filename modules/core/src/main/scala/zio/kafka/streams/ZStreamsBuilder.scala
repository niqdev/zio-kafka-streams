package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.scala.{ ByteArrayKeyValueStore, StreamsBuilder }
import zio._

// TODO incomplete: globalTable, addStateStore, addGlobalStore, build(props)
// TODO newtype/refined ?
sealed abstract class ZStreamsBuilder(private val builder: StreamsBuilder) {

  def streamConsumed[K, V](topic: String): Consumed[K, V] => RIO[KafkaStreamsConfig, ZKStream[K, V]] =
    consumed =>
      for {
        config <- KafkaStreamsConfig.config
        stream <- ZKStream {
          val stream = builder.stream(topic)(consumed)
          if (config.debug) stream.print(Printed.toSysOut[K, V].withLabel(topic))
          stream
        }
      } yield stream

  /**
    * Creates a [[ZKStream]] from the specified topic name,
    * requires an implicit instance of [[RecordConsumed]].
    */
  def stream[K, V](topic: String)(
    implicit C: RecordConsumed[K, V]
  ): RIO[KafkaStreamsConfig, ZKStream[K, V]] =
    streamConsumed(topic)(C.consumed)

  /**
    * Creates a [[ZKStream]] from the specified topic name,
    * requires an implicit instance of [[AvroRecordConsumed]].
    */
  def streamAvro[K, V](topic: String)(
    implicit C: AvroRecordConsumed[K, V]
  ): RIO[KafkaStreamsConfig, ZKStream[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl => streamConsumed(topic)(C.consumed(schemaRegistryUrl)))

  /**
    * Creates a [[ZKTable]] from the specified topic name,
    * requires an implicit instance of [[RecordConsumed]] and [[RecordMaterialized]].
    */
  def table[K, V](topic: String)(
    implicit C: RecordConsumed[K, V],
    M: RecordMaterialized[K, V, ByteArrayKeyValueStore]
  ): RIO[KafkaStreamsConfig, ZKTable[K, V]] =
    ZKTable(builder.table(topic, M.materialize)(C.consumed))

  /**
    * Creates a [[ZKTable]] from the specified topic name,
    * requires an implicit instance of [[AvroRecordConsumed]] and [[AvroRecordMaterialized]].
    */
  def tableAvro[K, V](topic: String)(
    implicit C: AvroRecordConsumed[K, V],
    M: AvroRecordMaterialized[K, V, ByteArrayKeyValueStore]
  ): RIO[KafkaStreamsConfig, ZKTable[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZKTable(builder.table(topic, M.materialize(schemaRegistryUrl))(C.consumed(schemaRegistryUrl)))
      )
}

object ZStreamsBuilder {

  def newInstance: Task[ZStreamsBuilder] =
    Task.effect(new ZStreamsBuilder(new StreamsBuilder()) {})

  /**
    * Uses monadic composition to builds a [[Topology]] with [[ZKStream]] and [[ZKTable]].
    */
  def apply(f: ZStreamsBuilder => RIO[KafkaStreamsConfig, Unit]): RIO[KafkaStreamsConfig, Topology] =
    newInstance.flatMap(zsb => f(zsb) *> Task.effect(zsb.builder.build()))
}
