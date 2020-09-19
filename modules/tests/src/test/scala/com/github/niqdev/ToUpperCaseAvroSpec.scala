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
    ZTestTopology.testConfigLayer() ++ ToUpperCaseConfig.customConfigLayer >+>
      KafkaStreamsTopology.make(ToUpperCaseAvroTopology.topology)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ToUpperCaseAvroSpec")(
      testM("topology") {
        checkM(Gen.anyUUID, Gen.anyString) { (inputKey, inputValue) =>
          for {
            sourceTopic <- CustomConfig.sourceTopic
            sinkTopic   <- CustomConfig.sinkTopic
            outputValue <- ZTestTopology.driver.use { driver =>
              for {
                input      <- driver.createAvroInput[DummyKey, DummyValue](sourceTopic)
                output     <- driver.createAvroOutput[DummyKey, DummyValue](sinkTopic)
                _          <- input.produce(DummyKey(inputKey), DummyValue(inputValue))
                dummyValue <- output.consumeValue
              } yield dummyValue.value
            }
          } yield assert(outputValue)(equalTo(inputValue.toUpperCase))
        }
        // specify [TestEnvironment] or it won't compile!
      }.provideSomeLayerShared[TestEnvironment](testLayer.mapError(TestFailure.fail))
    )
}
