package com.github.niqdev

import org.apache.kafka.streams.Topology
import zio.config.ConfigDescriptor.string
import zio.config._
import zio.kafka.streams._
import zio.logging.{ Logging, log }
import zio.{ Layer, ZIO }

final case class MySettings(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: String,
  sourceTopic: String,
  sinkTopic: String
) extends KafkaStreamsSettings {
  override def extraProperties: Map[String, AnyRef] =
    Map.empty
}
object MySettings {

  // example auto derivation
//  final val configDescriptor0: ConfigDescriptor[MySettings] =
//    descriptor[MySettings]
  final val configDescriptor: ConfigDescriptor[MySettings] =
    (string("APPLICATION_ID") |@|
      string("BOOTSTRAP_SERVERS") |@|
      string("SCHEMA_REGISTRY_URL") |@|
      string("SOURCE_TOPIC") |@|
      string("SINK_TOPIC"))(
      MySettings.apply,
      MySettings.unapply
    )
  final val configEnvLayer: Layer[ReadError[String], ZConfig[MySettings]] =
    ZConfig.fromSystemEnv(configDescriptor)
  final val configLocalLayer: Layer[ReadError[String], ZConfig[MySettings]] =
    ZConfig.fromMap(
      Map(
        "APPLICATION_ID"      -> "to-upper-case",
        "BOOTSTRAP_SERVERS"   -> "localhost:9092",
        "SCHEMA_REGISTRY_URL" -> "http://localhost:8081",
        "SOURCE_TOPIC"        -> "example.source.v1",
        "SINK_TOPIC"          -> "example.sink.v1"
      ),
      configDescriptor
    )
}

object ToUpperCaseApp extends KafkaStreamsApp(MySettings.configLocalLayer) {

  // TODO ZKStream/ZKTable
  // TODO wrap into a service ZConfig[MySettings] to make it more generic/flexible
  override def runApp: ZIO[Logging with ZConfig[MySettings], Throwable, Topology] =
    for {
      _        <- log.info("TODO")
      settings <- ZIO.access[ZConfig[MySettings]](_.get)
      topology <- ZKStream.builder { builder =>
        import org.apache.kafka.streams.scala.ImplicitConversions.{ consumedFromSerde, producedFromSerde }
        import org.apache.kafka.streams.scala.Serdes.String

        val sourceStream    = builder.stream[String, String](settings.sourceTopic)(consumedFromSerde)
        val upperCaseStream = sourceStream.mapValues(_.toUpperCase())
        upperCaseStream.to(settings.sinkTopic)(producedFromSerde)
      }
    } yield topology
}
