package zio.kafka
package streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.streams.StreamsConfig
import zio._

object KafkaStreamsConfig {
  type KafkaStreamsConfig = Has[KafkaStreamsConfig.Service]

  trait Service {

    /**
      * Kafka Streams app configurations
      */
    def config: Task[AppConfig]
  }

  def make(appConfig: => Task[AppConfig]): ULayer[KafkaStreamsConfig] =
    ZLayer.succeed(new KafkaStreamsConfig.Service {
      override def config: Task[AppConfig] = appConfig
    })

  def config: RIO[KafkaStreamsConfig, AppConfig] =
    ZIO.accessM[KafkaStreamsConfig](_.get.config)

  def requiredSchemaRegistryUrl: RIO[KafkaStreamsConfig, String] =
    for {
      values <- ZIO.accessM[KafkaStreamsConfig](_.get.config)
      schemaRegistryUrl <-
        values
          .schemaRegistryUrl
          .fold[Task[String]](Task.fail(new IllegalArgumentException("Missing schemaRegistryUrl")))(value =>
            Task.succeed(value)
          )
    } yield schemaRegistryUrl
}

final case class AppConfig(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: Option[String],
  shutdownTimeout: Long,
  debug: Boolean,
  private val properties: Map[String, AnyRef]
) {

  def withProperty(key: String, value: AnyRef): AppConfig =
    copy(properties = properties + (key -> value))

  def withProperties(props: (String, AnyRef)*): AppConfig =
    withProperties(props.toMap)

  def withProperties(props: Map[String, AnyRef]): AppConfig =
    copy(properties = properties ++ props)

  def toJavaProperties: Task[java.util.Properties] =
    Task.effect(properties.foldLeft(new java.util.Properties()) { (props, kv) =>
      props.put(kv._1, kv._2)
      props
    })

  def prettyPrint: String =
    s"""
       |Application properties:
       |${properties.foldLeft("")((output, kv) => output + s"\t${kv._1} = ${kv._2}\n")}
       |Other configurations:
       |\tdebug = $debug
       |\tshutdownTimeout = $shutdownTimeout seconds
       |\n""".stripMargin
}
object AppConfig {
  // TODO change shutdownTimeout from Long to Duration
  def apply(
    applicationId: String,
    bootstrapServers: String,
    schemaRegistryUrl: Option[String] = None,
    shutdownTimeout: Long = 5, // seconds
    debug: Boolean = false
  ): AppConfig = {
    val baseProps = Map(
      StreamsConfig.APPLICATION_ID_CONFIG    -> applicationId,
      StreamsConfig.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers
    )
    val props = schemaRegistryUrl.fold(baseProps)(url =>
      baseProps + (AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> url)
    )
    AppConfig(applicationId, bootstrapServers, schemaRegistryUrl, shutdownTimeout, debug, props)
  }
}
