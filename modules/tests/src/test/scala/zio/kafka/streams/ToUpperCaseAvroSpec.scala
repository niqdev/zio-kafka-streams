package zio.kafka.streams

import kafka.streams.serde._
import org.apache.kafka.streams.TopologyTestDriver
import zio._
import zio.test._
import zio.test.environment._
import zio.test.Assertion._

object ToUpperCaseAvroSpec extends DefaultRunnableSpec {

  final case class DummyKey(int: Int)
  object DummyKey {
    final implicit val dummyKeyAvroCodec: AvroCodec[DummyKey] =
      AvroCodec.genericKey[DummyKey]
  }
  final case class DummyValue(value: String)
  object DummyValue {
    final implicit val dummyValueAvroCodec: AvroCodec[DummyValue] =
      AvroCodec.genericValue[DummyValue]
  }

  private[this] val testConfigLayer: ULayer[KafkaStreamsConfig] =
    KafkaStreamsConfig.make(
      UIO.succeed(
        AppConfig(
          applicationId = "test-app",
          bootstrapServers = "dummy:12345",
          schemaRegistryUrl = Some("mock://TODO"),
          debug = true
        )
      )
    )

  private[this] val topologyLayer: RLayer[KafkaStreamsConfig, KafkaStreamsTopology] =
    KafkaStreamsTopology.make {
      ZStreamsBuilder { builder =>
        for {
          sourceStream <- builder.streamAvro[DummyKey, DummyValue]("source-topic")
          sinkStream   <- sourceStream.mapValue(dummy => DummyValue(dummy.value.toUpperCase))
          _            <- sinkStream.toAvro("sink-topic")
        } yield ()
      }
    }

  private[this] val keySerializer     = AvroCodec[DummyKey].serde("mock://TODO").serializer()
  private[this] val keyDeserializer   = AvroCodec[DummyKey].serde("mock://TODO").deserializer()
  private[this] val valueSerializer   = AvroCodec[DummyValue].serde("mock://TODO").serializer()
  private[this] val valueDeserializer = AvroCodec[DummyValue].serde("mock://TODO").deserializer()

  private[this] val setup: ZIO[KafkaStreamsTopology with KafkaStreamsConfig, Throwable, TopologyTestDriver] =
    for {
      properties <- KafkaStreamsConfig.config.flatMap(_.toJavaProperties)
      topology   <- KafkaStreamsTopology.build
      driver     <- Task.effect(new TopologyTestDriver(topology, properties))
    } yield driver

  private[this] def makeDriver
    : ZManaged[KafkaStreamsTopology with KafkaStreamsConfig, Throwable, TopologyTestDriver] =
    ZManaged.make(setup)(driver => Task.effect(driver.close()).catchAll(_ => ZIO.unit))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ToUpperCaseAvroSpec")(
      testM("topology") {
        for {
          result <- makeDriver.use { driver =>
            Task.effect {
              val source = driver.createInputTopic("source-topic", keySerializer, valueSerializer)
              val sink   = driver.createOutputTopic("sink-topic", keyDeserializer, valueDeserializer)
              source.pipeInput(DummyKey(1), DummyValue("myValue"))
              sink.readValue()
            }
          }
        } yield assert(result.value)(equalTo("MYVALUE"))
      }.provideSomeLayerShared((testConfigLayer >+> topologyLayer).mapError(TestFailure.fail))
    )
}
