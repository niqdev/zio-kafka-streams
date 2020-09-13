package com.github.niqdev

import com.github.niqdev.ToUpperCaseConfig._
import org.apache.kafka.streams.Topology
import zio._
import zio.kafka.streams._

/*
 * Kafka Streams application
 */
object ToUpperCaseApp extends KafkaStreamsApp(ToUpperCaseTopology.layer)

/*
 * Topology
 */
object ToUpperCaseTopology {
  private[this] lazy val topology: RIO[KafkaStreamsConfig with CustomConfig, Topology] =
    for {
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
    ToUpperCaseConfig.layer >+> KafkaStreamsTopology.make(topology)
}

/*
 * Configurations
 */
final case class ToUpperCaseConfig(
  applicationId: String,
  bootstrapServers: String,
  sourceTopic: String,
  sinkTopic: String
)
object ToUpperCaseConfig {
  type CustomConfig = Has[CustomConfig.Service]

  object CustomConfig {
    trait Service {
      def sourceTopic: Task[String]
      def sinkTopic: Task[String]
    }

    def sourceTopic: RIO[CustomConfig, String] =
      ZIO.accessM[CustomConfig](_.get.sourceTopic)
    def sinkTopic: RIO[CustomConfig, String] =
      ZIO.accessM[CustomConfig](_.get.sinkTopic)
  }

  private[this] lazy val configLayer: ULayer[KafkaStreamsConfig] =
    KafkaStreamsConfig.make(
      UIO.succeed(
        AppConfig(
          applicationId = "to-upper-case",
          bootstrapServers = "localhost:9092",
          debug = true
        )
      )
    )

  private[this] lazy val customConfigLayer: ULayer[CustomConfig] =
    ZLayer.succeed(new CustomConfig.Service {
      override def sourceTopic: Task[String] =
        UIO.succeed("example.source.v1")
      override def sinkTopic: Task[String] =
        UIO.succeed("example.sink.v1")
    })

  lazy val layer: ULayer[KafkaStreamsConfig with CustomConfig] =
    configLayer ++ customConfigLayer
}
