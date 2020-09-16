package kafka.streams.serde

import com.sksamuel.avro4s.{ Decoder, Encoder, RecordFormat }
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.Serdes

import scala.jdk.CollectionConverters.MapHasAsJava

// TODO @implicitNotFound
trait AvroCodec[T] {
  def serde(schemaRegistryUrl: String): Serde[T]
}

object AvroCodec {
  def apply[T](implicit ev: AvroCodec[T]): AvroCodec[T] = ev

  def generic[T >: Null: Encoder: Decoder](
    isKey: Boolean,
    serdeConfig: Map[String, AnyRef] = Map.empty,
    schemaRegistryClient: Option[String => SchemaRegistryClient] = None
  ): AvroCodec[T] =
    schemaRegistryUrl => {

      val recordFormat = RecordFormat[T]
      val schemaRegistryConfig =
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
      val serde =
        schemaRegistryClient.fold(new GenericAvroSerde)(f => new GenericAvroSerde(f(schemaRegistryUrl)))
      serde.configure((serdeConfig + schemaRegistryConfig).asJava, isKey)

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

  /**
    * TODO docs
    */
  def genericKey[T >: Null: Encoder: Decoder]: AvroCodec[T] =
    generic(isKey = true)

  /**
    * TODO docs
    */
  def genericValue[T >: Null: Encoder: Decoder]: AvroCodec[T] =
    generic(isKey = false)
}
