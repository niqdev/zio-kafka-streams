package zio.kafka.streams
package testkit

import kafka.streams.serde._
import org.apache.kafka.streams.{ Topology, TopologyTestDriver }
import zio._
import zio.random.Random
import zio.test.Gen

sealed abstract class ZTestTopology(private val driver: TopologyTestDriver) {

  def createInput[K, V](topic: String)(
    implicit K: Codec[K],
    V: Codec[V]
  ): Task[ZTestInput[K, V]] =
    ZTestInput(driver.createInputTopic(topic, K.serde.serializer, V.serde.serializer))

  def createAvroInput[K, V](topic: String)(
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestInput[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestInput(
          driver.createInputTopic(
            topic,
            K.serde(schemaRegistryUrl).serializer,
            V.serde(schemaRegistryUrl).serializer
          )
        )
      )

  def createAvroValueInput[K, V](topic: String)(
    implicit K: Codec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestInput[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestInput(
          driver.createInputTopic(
            topic,
            K.serde.serializer,
            V.serde(schemaRegistryUrl).serializer
          )
        )
      )

  def createOutput[K, V](topic: String)(
    implicit K: Codec[K],
    V: Codec[V]
  ): Task[ZTestOutput[K, V]] =
    ZTestOutput(driver.createOutputTopic(topic, K.serde.deserializer, V.serde.deserializer))

  def createAvroOutput[K, V](topic: String)(
    implicit K: AvroCodec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestOutput[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestOutput(
          driver.createOutputTopic(
            topic,
            K.serde(schemaRegistryUrl).deserializer,
            V.serde(schemaRegistryUrl).deserializer
          )
        )
      )

  def createAvroValueOutput[K, V](topic: String)(
    implicit K: Codec[K],
    V: AvroCodec[V]
  ): RIO[KafkaStreamsConfig, ZTestOutput[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl =>
        ZTestOutput(
          driver.createOutputTopic(
            topic,
            K.serde.deserializer,
            V.serde(schemaRegistryUrl).deserializer
          )
        )
      )
}

object ZTestTopology {

  val topicNameGen: Gen[Random, String] =
    Gen.anyUUID.map(_.toString)

  // "mock://" prefix is used internally by GenericAvroSerde to mock SchemaRegistryClient
  def testConfigLayer(debug: Boolean = false): ULayer[KafkaStreamsConfig] =
    KafkaStreamsConfig.make(
      UIO.succeed(
        AppConfig(
          applicationId = s"test-app-${java.util.UUID.randomUUID}",
          bootstrapServers = "TEST:12345",
          schemaRegistryUrl = Some("mock://TEST"),
          debug = debug
        )
      )
    )

  def testLayer(
    topology: RIO[KafkaStreamsConfig, Topology]
  ): TaskLayer[KafkaStreamsConfig with KafkaStreamsTopology] =
    ZTestTopology.testConfigLayer() >+> KafkaStreamsTopology.make(topology)

  private[this] lazy val setup: RIO[KafkaStreamsTopology with KafkaStreamsConfig, TopologyTestDriver] =
    for {
      properties <- KafkaStreamsConfig.config.flatMap(_.toJavaProperties)
      topology   <- KafkaStreamsTopology.build
      driver     <- Task.effect(new TopologyTestDriver(topology, properties))
    } yield driver

  private[this] lazy val stop: TopologyTestDriver => UIO[Unit] =
    driver => Task.effect(driver.close()).ignore

  /**
    * TODO docs
    */
  def driver: RManaged[KafkaStreamsTopology with KafkaStreamsConfig, ZTestTopology] =
    ZManaged.make(setup)(stop).map(d => new ZTestTopology(d) {})

  /**
    * TODO docs
    */
  def testSingleMessage[K: Codec, V: Codec](
    inputTopic: String,
    outputTopic: String,
    key: K,
    value: V
  ): RIO[KafkaStreamsTopology with KafkaStreamsConfig, (K, V)] =
    ZTestTopology
      .driver
      .use { driver =>
        for {
          input    <- driver.createInput[K, V](inputTopic)
          output   <- driver.createOutput[K, V](outputTopic)
          _        <- input.produce(key, value)
          keyValue <- output.consume
        } yield keyValue
      }

  /**
    * TODO docs
    */
  def testSingleAvroMessage[K: AvroCodec, V: AvroCodec](
    inputTopic: String,
    outputTopic: String,
    key: K,
    value: V
  ): RIO[KafkaStreamsTopology with KafkaStreamsConfig, (K, V)] =
    ZTestTopology
      .driver
      .use { driver =>
        for {
          input    <- driver.createAvroInput[K, V](inputTopic)
          output   <- driver.createAvroOutput[K, V](outputTopic)
          _        <- input.produce(key, value)
          keyValue <- output.consume
        } yield keyValue
      }
}
