package zio.kafka.streams
package testkit

import kafka.streams.serde._
import org.apache.kafka.streams.TopologyTestDriver
import zio._

sealed abstract class ZTestTopology(private val driver: TopologyTestDriver) {

  def source[K, V](topic: String)(
    implicit K: Codec[K],
    V: Codec[V]
  ): Task[ZTestSource[K, V]] =
    ZTestSource(driver.createInputTopic(topic, K.serde.serializer, V.serde.serializer))

  def sourceAvro[K, V](topic: String)(
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestSource[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestSource(
          driver.createInputTopic(
            topic,
            K.serde(schemaRegistryUrl).serializer,
            V.serde(schemaRegistryUrl).serializer
          )
        )
      )

  def sourceAvroValue[K, V](topic: String)(
    implicit K: Codec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestSource[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestSource(
          driver.createInputTopic(
            topic,
            K.serde.serializer,
            V.serde(schemaRegistryUrl).serializer
          )
        )
      )

  def sink[K, V](topic: String)(
    implicit K: Codec[K],
    V: Codec[V]
  ): Task[ZTestSink[K, V]] =
    ZTestSink(driver.createOutputTopic(topic, K.serde.deserializer, V.serde.deserializer))

  def sinkAvro[K, V](topic: String)(
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestSink[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestSink(
          driver.createOutputTopic(
            topic,
            K.serde(schemaRegistryUrl).deserializer,
            V.serde(schemaRegistryUrl).deserializer
          )
        )
      )

  def sinkAvroValue[K, V](topic: String)(
    implicit K: Codec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestSink[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestSink(
          driver.createOutputTopic(
            topic,
            K.serde.deserializer,
            V.serde(schemaRegistryUrl).deserializer
          )
        )
      )
}

object ZTestTopology {

  // "mock://" prefix is used internally by GenericAvroSerde
  val testConfigLayer: ULayer[KafkaStreamsConfig] =
    KafkaStreamsConfig.make(
      UIO.succeed(
        AppConfig(
          applicationId = s"test-app-${java.util.UUID.randomUUID}",
          bootstrapServers = "TEST:12345",
          schemaRegistryUrl = Some("mock://TEST"),
          debug = true
        )
      )
    )

  private[this] lazy val setup: RIO[KafkaStreamsTopology with KafkaStreamsConfig, TopologyTestDriver] =
    for {
      properties <- KafkaStreamsConfig.config.flatMap(_.toJavaProperties)
      topology   <- KafkaStreamsTopology.build
      driver     <- Task.effect(new TopologyTestDriver(topology, properties))
    } yield driver

  private[this] lazy val stop: TopologyTestDriver => UIO[Unit] =
    driver => Task.effect(driver.close()).ignore

  def driver: RManaged[KafkaStreamsTopology with KafkaStreamsConfig, ZTestTopology] =
    ZManaged.make(setup)(stop).map(d => new ZTestTopology(d) {})
}
