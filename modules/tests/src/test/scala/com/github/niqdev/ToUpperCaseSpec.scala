package com.github.niqdev

import com.github.niqdev.ToUpperCaseConfig.CustomConfig
import zio._
import zio.kafka.streams._
import zio.kafka.streams.testkit._
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

object ToUpperCaseSpec extends DefaultRunnableSpec {

  private[this] val testLayer: TaskLayer[KafkaStreamsConfig with CustomConfig with KafkaStreamsTopology] =
    ZTestTopology.testConfigLayer ++ ToUpperCaseConfig.customConfigLayer >+>
      KafkaStreamsTopology.make(ToUpperCaseTopology.topology)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ToUpperCaseSpec")(
      testM("topology") {
        for {
          sourceTopic <- CustomConfig.sourceTopic
          sinkTopic   <- CustomConfig.sinkTopic
          result <- ZTestTopology.driver.use { driver =>
            for {
              input      <- driver.createInput[String, String](sourceTopic)
              output     <- driver.createOutput[String, String](sinkTopic)
              _          <- input.produceValue("myValue")
              dummyValue <- output.consumeValue
            } yield dummyValue
          }
        } yield assert(result)(equalTo("MYVALUE"))
      }.provideSomeLayerShared(testLayer.mapError(TestFailure.fail))
    )
}
