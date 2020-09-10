package com.github.niqdev

import org.apache.kafka.streams.Topology
import zio._
import zio.config.ConfigDescriptor.string
import zio.config._
import zio.kafka.streams.Settings.KafkaStreamsSettings
import zio.kafka.streams._
import zio.logging._

final case class MySettings(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: String,
  sourceTopic: String,
  sinkTopic: String
) extends KafkaStreamsSettings
object MySettings {

  final val configDescriptor: ConfigDescriptor[MySettings] =
    (string("APPLICATION_ID") |@|
      string("BOOTSTRAP_SERVERS") |@|
      string("SCHEMA_REGISTRY_URL") |@|
      string("SOURCE_TOPIC") |@|
      string("SINK_TOPIC"))(
      MySettings.apply,
      MySettings.unapply
    )

  val liveLocal = new Settings.Service[MySettings] {
    private[this] val configMap =
      Map(
        "APPLICATION_ID"      -> "to-upper-case",
        "BOOTSTRAP_SERVERS"   -> "localhost:9092",
        "SCHEMA_REGISTRY_URL" -> "http://localhost:8081",
        "SOURCE_TOPIC"        -> "example.source.v1",
        "SINK_TOPIC"          -> "example.sink.v1"
      )

    override def config: Task[MySettings] =
      ZIO.fromEither(read(configDescriptor from ConfigSource.fromMap(configMap)))
  }

  val liveEnv = new Settings.Service[MySettings] {
    override def config: Task[MySettings] =
      ConfigSource
        .fromSystemEnv
        .flatMap(configSource => ZIO.fromEither(read(configDescriptor from configSource)))
  }
}

object ToUpperCaseApp extends KafkaStreamsApp(ZLayer.succeed(MySettings.liveLocal)) {

  // TODO custom settings
  override def runApp: RIO[Logging with Settings, Topology] =
    for {
      settings <- Settings.config[MySettings]
      _        <- log.info(s"Running ${settings.applicationId}")
      topology <- ZStreamsBuilder { builder =>
        for {
          sourceStream    <- builder.stream[String, String](settings.sourceTopic)
          upperCaseStream <- sourceStream.mapValues(_.toUpperCase)
          _               <- upperCaseStream.to(settings.sinkTopic)
        } yield ()
      }
    } yield topology
}
