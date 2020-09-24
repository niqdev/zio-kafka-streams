package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import zio._

// TODO incomplete + tests + docs
// TODO ZKGroupedTable
sealed abstract class ZKTable[K, V](private val table: KTable[K, V]) {

  def joinAvro[VO, VR](other: ZKTable[K, VO])(joiner: (V, VO) => VR)(
    implicit M: AvroRecordMaterialized[K, VR, ByteArrayKeyValueStore]
  ): RIO[KafkaStreamsConfig, ZKTable[K, VR]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZKTable(table.join(other.table, M.materialize(schemaRegistryUrl))(joiner))
      )

  def toStream: Task[ZKStream[K, V]] =
    ZKStream(table.toStream)
}
object ZKTable {

  def apply[K, V](table: KTable[K, V]): Task[ZKTable[K, V]] =
    Task.effect(new ZKTable[K, V](table) {})
}
