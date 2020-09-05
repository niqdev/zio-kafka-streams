package zio.kafka.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.streams.StreamsConfig
import zio.Layer
import zio.config.ConfigDescriptor.string
import zio.config._

import scala.jdk.CollectionConverters.MapHasAsJava

// TODO handle custom properties
object settings {

  final case class Settings(
    applicationId: String,
    bootstrapServers: String,
    schemaRegistryUrl: String
  ) {
    // TODO ZIO[x, y, Properties] ?
    def properties(customProps: Map[String, AnyRef] = Map.empty): java.util.Properties = {
      val props = new java.util.Properties()
      props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
      props.putAll(customProps.asJava)
      props
    }
  }

  object Settings {
    final val descriptor: ConfigDescriptor[Settings] =
      (string("APPLICATION_ID") |@|
        string("BOOTSTRAP_SERVERS") |@|
        string("SCHEMA_REGISTRY_URL"))(
        Settings.apply,
        Settings.unapply
      )

    final val configLocalLayer: Layer[ReadError[String], ZConfig[Settings]] =
      ZConfig.fromMap(
        Map(
          "APPLICATION_NAME"    -> "zio-kafka-streams",
          "BOOTSTRAP_SERVERS"   -> "localhost:9092",
          "SCHEMA_REGISTRY_URL" -> "http://localhost:8081"
        ),
        descriptor
      )

    final val configEnvLayer: Layer[ReadError[String], ZConfig[Settings]] =
      ZConfig.fromSystemEnv(descriptor)
  }
}
