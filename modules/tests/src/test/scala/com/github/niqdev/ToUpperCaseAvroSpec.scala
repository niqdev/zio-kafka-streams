package com.github.niqdev

import com.github.niqdev.ToUpperCaseConfig.CustomConfig
import com.github.niqdev.schema.dummy._
import zio._
import zio.kafka.streams._
import zio.kafka.streams.testkit._
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

object ToUpperCaseAvroSpec extends DefaultRunnableSpec {

  private[this] val testLayer: TaskLayer[KafkaStreamsConfig with CustomConfig with KafkaStreamsTopology] =
    ZTestTopology.testConfigLayer ++ ToUpperCaseConfig.customConfigLayer >+>
      KafkaStreamsTopology.make(ToUpperCaseAvroTopology.topology)

  // TODO use check
  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ToUpperCaseAvroSpec")(
      testM("topology") {
        for {
          sourceTopic <- CustomConfig.sourceTopic
          sinkTopic   <- CustomConfig.sinkTopic
          result <- ZTestTopology.driver.use { driver =>
            for {
              source     <- driver.sourceAvro[DummyKey, DummyValue](sourceTopic)
              sink       <- driver.sinkAvro[DummyKey, DummyValue](sinkTopic)
              _          <- source.produce(DummyKey(java.util.UUID.randomUUID), DummyValue("myValue"))
              dummyValue <- sink.consumeValue
            } yield dummyValue
          }
        } yield assert(result.value)(equalTo("MYVALUE"))
      }.provideSomeLayerShared(testLayer.mapError(TestFailure.fail))
    )
}
