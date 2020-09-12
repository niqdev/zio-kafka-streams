package com.github.niqdev

import com.github.niqdev.MySettings.CustomSettings.CustomSettings
import org.apache.kafka.streams.Topology
import zio._
import zio.config.ConfigDescriptor.string
import zio.config._
import zio.kafka.streams._
import zio.logging._

object MyTopology {

  val app: RIO[Settings with CustomSettings with Logging, Topology] =
    for {
      settings    <- Settings.settings
      sourceTopic <- MySettings.CustomSettings.sourceTopic
      sinkTopic   <- MySettings.CustomSettings.sourceTopic
      _           <- log.info(s"Running ${settings.applicationId}")
      topology <- ZStreamsBuilder { builder =>
        for {
          sourceStream    <- builder.stream[String, String](sourceTopic)
          upperCaseStream <- sourceStream.mapValues(_.toUpperCase)
          _               <- upperCaseStream.to(sinkTopic)
        } yield ()
      }
    } yield topology

  val topologyLayer =
    Logging.console() ++ MySettings.localSettingsLayer >>> ZKSTopology.make(app) ++
      ZLayer.succeed(MySettings.liveLocalSettings)
}

object ToUpperCaseApp extends KafkaStreamsApp(MyTopology.topologyLayer)

final case class MySettings(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: String,
  sourceTopic: String,
  sinkTopic: String
)
object MySettings {

  object CustomSettings {
    type CustomSettings = Has[CustomSettings.Service]

    trait Service {
      def sourceTopic: Task[String]
      def sinkTopic: Task[String]
    }

    def sourceTopic: RIO[CustomSettings, String] =
      ZIO.accessM[CustomSettings](_.get.sourceTopic)
    def sinkTopic: RIO[CustomSettings, String] =
      ZIO.accessM[CustomSettings](_.get.sinkTopic)
  }

  private[this] val configDescriptor: ConfigDescriptor[MySettings] =
    (string("APPLICATION_ID") |@|
      string("BOOTSTRAP_SERVERS") |@|
      string("SCHEMA_REGISTRY_URL") |@|
      string("SOURCE_TOPIC") |@|
      string("SINK_TOPIC"))(
      MySettings.apply,
      MySettings.unapply
    )

  private[this] val configToSettings: MySettings => AppSettings =
    config =>
      AppSettings(
        applicationId = config.applicationId,
        bootstrapServers = config.bootstrapServers,
        debug = true
      )

  private[this] val mySettingsLocal: IO[ReadError[String], MySettings] = {
    val configMap =
      Map(
        "APPLICATION_ID"      -> "to-upper-case",
        "BOOTSTRAP_SERVERS"   -> "localhost:9092",
        "SCHEMA_REGISTRY_URL" -> "http://localhost:8081",
        "SOURCE_TOPIC"        -> "example.source.v1",
        "SINK_TOPIC"          -> "example.sink.v1"
      )
    ZIO.fromEither(read(configDescriptor from ConfigSource.fromMap(configMap)))
  }

  private[this] val mySettingsEnv =
    ConfigSource
      .fromSystemEnv
      .flatMap(configSource => ZIO.fromEither(read(configDescriptor from configSource)))

  val liveLocalSettings: Settings.Service = new Settings.Service {
    override def settings: Task[AppSettings] =
      mySettingsLocal.map(configToSettings)
  }

  val liveEnvSettings = new Settings.Service {
    override def settings: Task[AppSettings] =
      mySettingsEnv.map(configToSettings)
  }

  val liveLocalCustomSettings = new CustomSettings.Service {
    override def sourceTopic: Task[String] =
      mySettingsLocal.map(_.sourceTopic)
    override def sinkTopic: Task[String] =
      mySettingsLocal.map(_.sinkTopic)
  }

  val liveEnvCustomSettings = new CustomSettings.Service {
    override def sourceTopic: Task[String] =
      mySettingsEnv.map(_.sourceTopic)
    override def sinkTopic: Task[String] =
      mySettingsEnv.map(_.sinkTopic)
  }

  val localSettingsLayer =
    ZLayer.succeed(liveLocalSettings) ++ ZLayer.succeed(liveLocalCustomSettings)

  val envSettingsLayer =
    ZLayer.succeed(liveEnvSettings) ++ ZLayer.succeed(liveEnvCustomSettings)
}
