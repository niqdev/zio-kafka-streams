package com.github.niqdev

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import zio.kafka.streams.KafkaStreamsApp
import zio.kafka.streams.settings.AppSettings
import zio.logging.Logger
import zio.{ Task, ZIO }

final case class TopologySettings(
  userSource: String,
  repositorySource: String,
  gitHubSink: String
)

object ExampleZIOApp extends KafkaStreamsApp[TopologySettings] {

  override def topology(
    log: Logger[String],
    appSettings: AppSettings,
    topologySettings: TopologySettings
  ): Task[Topology] =
    for {
      _ <- log.info("Build topology ...")
      _ <- log.info(s"schemaRegistryUrl: ${appSettings.schemaRegistryUrl}")
      topology <- ZIO.effect {
        import org.apache.kafka.streams.scala.ImplicitConversions.{ consumedFromSerde, producedFromSerde }
        import org.apache.kafka.streams.scala.Serdes.String

        val builder = new StreamsBuilder()

        val sourceStream    = builder.stream[String, String](topologySettings.userSource)(consumedFromSerde)
        val upperCaseStream = sourceStream.mapValues(_.toUpperCase())
        upperCaseStream.to(topologySettings.gitHubSink)(producedFromSerde)

        builder.build()
      }
    } yield topology
}
