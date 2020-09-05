package zio.kafka.streams

import java.util.{ Properties => JProperties }

import zio._
import zio.config._

object KafkaStreamsSettings {
  type KafkaStreamsSettings = Has[KafkaStreamsSettings.Service]

  // TODO newtype + refined ?
  trait Settings {
    def applicationId: String
    def bootstrapServers: String
    def schemaRegistryUrl: String
  }

  trait Service {
    def config[T <: Settings]: UIO[ConfigDescriptor[T]]
    def properties: UIO[JProperties]
  }
  // TODO
  object Service {}

  def config[T <: Settings]: RIO[KafkaStreamsSettings, ConfigDescriptor[T]] =
    ZIO.accessM[KafkaStreamsSettings](_.get.config)

  def properties: RIO[KafkaStreamsSettings, JProperties] =
    ZIO.accessM[KafkaStreamsSettings](_.get.properties)
}
