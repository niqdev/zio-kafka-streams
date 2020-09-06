package zio.kafka.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.streams.StreamsConfig

import scala.jdk.CollectionConverters.MapHasAsJava

trait KafkaStreamsSettings {
  def applicationId: String
  def bootstrapServers: String
  def schemaRegistryUrl: String

  def extraProperties: Map[String, AnyRef] = Map.empty
  def properties: java.util.Properties = {
    val props = new java.util.Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
    props.putAll(extraProperties.asJava)
    props
  }
}
