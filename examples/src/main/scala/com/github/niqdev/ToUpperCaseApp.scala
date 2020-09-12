package com.github.niqdev

import com.github.niqdev.ToUpperCaseConfig._
import org.apache.kafka.streams.Topology
import zio._
import zio.config.ConfigDescriptor.string
import zio.config._
import zio.kafka.streams._
import zio.logging._

object ToUpperCaseTopology {

  private[this] lazy val app: RIO[KafkaStreamsConfig with CustomConfig with Logging, Topology] =
    for {
      config      <- KafkaStreamsConfig.config
      _           <- log.info(s"Running ${config.applicationId}")
      _           <- CustomConfig.prettyPrint.flatMap(values => log.info(values))
      sourceTopic <- CustomConfig.sourceTopic
      sinkTopic   <- CustomConfig.sinkTopic
      topology <- ZStreamsBuilder { builder =>
        for {
          sourceStream <- builder.stream[String, String](sourceTopic)
          sinkStream   <- sourceStream.mapValues(_.toUpperCase)
          _            <- sinkStream.to(sinkTopic)
        } yield ()
      }
    } yield topology

  val layer: RLayer[ZEnv, KafkaStreamsTopology with KafkaStreamsConfig] =
    Logging.console() ++ ToUpperCaseConfig.localLayer >>> KafkaStreamsTopology.make(app) ++
      ToUpperCaseConfig.localConfigLayer
}

object ToUpperCaseApp extends KafkaStreamsApp(ToUpperCaseTopology.layer)

final case class ToUpperCaseConfig(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: String,
  sourceTopic: String,
  sinkTopic: String
)
object ToUpperCaseConfig {
  type CustomConfig = Has[CustomConfig.Service]

  object CustomConfig {
    trait Service {
      def sourceTopic: Task[String]
      def sinkTopic: Task[String]
      def prettyPrint: Task[String]
    }

    def sourceTopic: RIO[CustomConfig, String] =
      ZIO.accessM[CustomConfig](_.get.sourceTopic)
    def sinkTopic: RIO[CustomConfig, String] =
      ZIO.accessM[CustomConfig](_.get.sinkTopic)
    def prettyPrint: RIO[CustomConfig, String] =
      ZIO.accessM[CustomConfig](_.get.prettyPrint)
  }

  private[this] lazy val configDescriptor: ConfigDescriptor[ToUpperCaseConfig] =
    (string("APPLICATION_ID") |@|
      string("BOOTSTRAP_SERVERS") |@|
      string("SCHEMA_REGISTRY_URL") |@|
      string("SOURCE_TOPIC") |@|
      string("SINK_TOPIC"))(
      ToUpperCaseConfig.apply,
      ToUpperCaseConfig.unapply
    )

  private[this] lazy val toAppConfig: ToUpperCaseConfig => AppConfig =
    config =>
      AppConfig(
        applicationId = config.applicationId,
        bootstrapServers = config.bootstrapServers,
        debug = true
      )

  private[this] lazy val localConfig: IO[ReadError[String], ToUpperCaseConfig] = {
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

  private[this] lazy val envConfig =
    ConfigSource
      .fromSystemEnv
      .flatMap(configSource => ZIO.fromEither(read(configDescriptor from configSource)))

  lazy val localConfigLayer =
    ZLayer.succeed(new KafkaStreamsConfig.Service {
      override def config: Task[AppConfig] =
        localConfig.map(toAppConfig)
    })
  lazy val localCustomLayer =
    ZLayer.succeed(new CustomConfig.Service {
      override def sourceTopic: Task[String] =
        localConfig.map(_.sourceTopic)
      override def sinkTopic: Task[String] =
        localConfig.map(_.sinkTopic)
      override def prettyPrint: Task[String] =
        localConfig
          .flatMap(values => UIO(s"""
               |LOCAL custom configurations
               |SOURCE_TOPIC: ${values.sourceTopic}
               |SINK_TOPIC: ${values.sinkTopic}
               |""".stripMargin))
          .absorb
    })

  lazy val envConfigLayer =
    ZLayer.succeed(new KafkaStreamsConfig.Service {
      override def config: Task[AppConfig] =
        envConfig.map(toAppConfig)
    })
  lazy val envCustomLayer =
    ZLayer.succeed(new CustomConfig.Service {
      override def sourceTopic: Task[String] =
        envConfig.map(_.sourceTopic)
      override def sinkTopic: Task[String] =
        envConfig.map(_.sinkTopic)
      override def prettyPrint: Task[String] =
        envConfig
          .flatMap(values => UIO(s"""
               |ENV custom configurations
               |SOURCE_TOPIC: ${values.sourceTopic}
               |SINK_TOPIC: ${values.sinkTopic}
               |""".stripMargin))
          .absorb
    })

  lazy val localLayer = localConfigLayer ++ localCustomLayer
  lazy val envLayer   = envConfigLayer ++ envCustomLayer
}
