package zio.kafka
package streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.streams.StreamsConfig
import zio._

// TODO Refined ?
object Settings {
  type Settings = Has[Settings.Service]

  trait Service {
    def settings: Task[AppSettings]
  }

  def settings: RIO[Settings, AppSettings] =
    ZIO.accessM[Settings](_.get.settings)
}

final case class AppSettings(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: Option[String],
  shutdownTimeout: Long,
  debug: Boolean,
  private val properties: Map[String, AnyRef]
) {

  def withProperty(key: String, value: AnyRef): AppSettings =
    copy(properties = properties + (key -> value))

  def withProperties(props: (String, AnyRef)*): AppSettings =
    withProperties(props.toMap)

  def withProperties(props: Map[String, AnyRef]): AppSettings =
    copy(properties = properties ++ props)

  def toJavaProperties: Task[java.util.Properties] =
    Task.effect(properties.foldLeft(new java.util.Properties()) { (props, kv) =>
      props.put(kv._1, kv._2)
      props
    })

  // TODO
  def prettyPrint: String =
    properties.iterator.mkString("\n")
}
object AppSettings {
  def apply(
    applicationId: String,
    bootstrapServers: String,
    schemaRegistryUrl: Option[String] = None,
    shutdownTimeout: Long = 5,
    debug: Boolean = false
  ): AppSettings = {
    val baseProps = Map(
      StreamsConfig.APPLICATION_ID_CONFIG    -> applicationId,
      StreamsConfig.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers
    )
    val props = schemaRegistryUrl.fold(baseProps)(url =>
      baseProps + (AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> url)
    )
    AppSettings(applicationId, bootstrapServers, schemaRegistryUrl, shutdownTimeout, debug, props)
  }
}
