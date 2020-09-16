package zio.kafka.streams

import kafka.streams.serde._
import org.apache.kafka.streams.TopologyTestDriver
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ToUpperCaseSpec extends DefaultRunnableSpec {

  private[this] val testConfigLayer: ULayer[KafkaStreamsConfig] =
    KafkaStreamsConfig.make(
      UIO.succeed(
        AppConfig(
          applicationId = "test-app",
          bootstrapServers = "localhost:9092",
          debug = true
        )
      )
    )

  private[this] val topologyLayer: RLayer[KafkaStreamsConfig, KafkaStreamsTopology] =
    KafkaStreamsTopology.make {
      ZStreamsBuilder { builder =>
        for {
          sourceStream <- builder.stream[String, String]("source-topic")
          sinkStream   <- sourceStream.mapValue(_.toUpperCase)
          _            <- sinkStream.to("sink-topic")
        } yield ()
      }
    }

  private[this] val stringSerializer   = Codec[String].serde.serializer()
  private[this] val stringDeserializer = Codec[String].serde.deserializer()

  private[this] val testDriver
    : ZIO[KafkaStreamsTopology with KafkaStreamsConfig, Throwable, TopologyTestDriver] =
    for {
      properties <- KafkaStreamsConfig.config.flatMap(_.toJavaProperties)
      topology   <- KafkaStreamsTopology.build
      driver     <- Task.effect(new TopologyTestDriver(topology, properties))
    } yield driver

  // TODO check(Gen.alphaNumericString)
  // TODO driver.close
  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ToUpperCaseSpec")(
      testM("topology") {
        for {
          (source, sink) <- testDriver.flatMap { driver =>
            Task.effect {
              val source = driver.createInputTopic("source-topic", stringSerializer, stringSerializer)
              val sink   = driver.createOutputTopic("sink-topic", stringDeserializer, stringDeserializer)
              (source, sink)
            }
          }
          _      <- Task.effect(source.pipeInput("myKey", "myValue"))
          result <- Task.effect(sink.readValue())
        } yield assert(result)(equalTo("MYVALUE"))
      }.provideSomeLayerShared((testConfigLayer >+> topologyLayer).mapError(TestFailure.fail))
    )
}
