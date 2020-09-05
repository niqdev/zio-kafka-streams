package kafka.streams.serde

import com.sksamuel.avro4s.{ Decoder, Encoder, RecordFormat }
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.Serdes

import scala.jdk.CollectionConverters.MapHasAsJava

// TODO @implicitNotFound
trait AvroCodec[T] {
  // TODO String Refined Url vs String ?
  def serde(schemaRegistry: String): Serde[T]
}

object AvroCodec {
  def apply[T](implicit ev: AvroCodec[T]): AvroCodec[T] = ev

  // TODO allow to add more props e.g. strategy
  def generic[T >: Null: Encoder: Decoder](isKey: Boolean): AvroCodec[T] =
    schemaRegistry => {

      val recordFormat = RecordFormat[T]
      val props        = Map(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistry).asJava
      val serde        = new GenericAvroSerde()
      serde.configure(props, isKey)

      val serializer: (String, T) => Array[Byte] =
        (topic, data) => serde.serializer().serialize(topic, recordFormat.to(data))

      val deserializer: (String, Array[Byte]) => Option[T] =
        (topic, bytes) =>
          Either
            .catchNonFatal(serde.deserializer().deserialize(topic, bytes))
            .map(recordFormat.from)
            .toOption

      Serdes.fromFn[T](serializer, deserializer)
    }
}
