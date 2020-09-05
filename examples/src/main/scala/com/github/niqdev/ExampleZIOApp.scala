package com.github.niqdev

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import zio.config.ConfigDescriptor.string
import zio.config._
import zio.kafka.streams.KafkaStreamsApp
import zio.kafka.streams.KafkaStreamsTopology.{ KafkaStreamsTopology, Service }
import zio.{ Layer, Task, ZIO, ZLayer }

final case class TopologySettings(
  userSource: String,
  repositorySource: String,
  gitHubSink: String
)
object TopologySettings {
  private[this] final val descriptor: ConfigDescriptor[TopologySettings] =
    (string("USER_SOURCE") |@|
      string("REPOSITORY_SOURCE") |@|
      string("GITHUB_SINK"))(
      TopologySettings.apply,
      TopologySettings.unapply
    )

  final val configEnvLayer: Layer[ReadError[String], ZConfig[TopologySettings]] =
    ZConfig.fromSystemEnv(descriptor)
}

object CustomTopology {

  val live: TopologySettings => Service =
    settings =>
      new Service {
        override def build: Task[Topology] =
          for {
            topology <- ZIO.effect {
              import org.apache.kafka.streams.scala.ImplicitConversions.{
                consumedFromSerde,
                producedFromSerde
              }
              import org.apache.kafka.streams.scala.Serdes.String

              val builder = new StreamsBuilder()

              val sourceStream    = builder.stream[String, String](settings.userSource)(consumedFromSerde)
              val upperCaseStream = sourceStream.mapValues(_.toUpperCase())
              upperCaseStream.to(settings.gitHubSink)(producedFromSerde)

              builder.build()
            }
          } yield topology
      }

  val topologyLayer: ZLayer[ZConfig[TopologySettings], Nothing, KafkaStreamsTopology] =
    ZLayer.fromService(CustomTopology.live)
}

object ExampleZIOApp
    extends KafkaStreamsApp(
      TopologySettings.configEnvLayer >+>
        CustomTopology.topologyLayer
    )
