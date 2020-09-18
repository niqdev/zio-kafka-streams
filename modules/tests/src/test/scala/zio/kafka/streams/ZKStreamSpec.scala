package zio.kafka.streams

import zio.kafka.streams.testkit.ZTestTopology
import zio.kafka.streams.testkit.ZTestTopology._
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

// TODO
object ZKStreamSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ZKStreamSpec")(
      testM("mapKey") {
        checkM(topicNameGen, topicNameGen, Gen.anyString, Gen.anyString) {
          (inputTopic, outputTopic, inputKey, inputValue) =>
            for {
              topology <- ZStreamsBuilder { builder =>
                for {
                  sourceStream <- builder.stream[String, String](inputTopic)
                  sinkStream   <- sourceStream.mapKey(_.toUpperCase)
                  _            <- sinkStream.to(outputTopic)
                } yield ()
              }.provideSomeLayer(ZTestTopology.testConfigLayer)
              (outputKey, _) <- testTopology(inputTopic, outputTopic, inputKey, inputValue)
                .provideSomeLayer[TestEnvironment](makeTestLayer(topology))
            } yield assert(outputKey)(equalTo(inputKey.toUpperCase))
        }
      },
      testM("mapValue") {
        checkM(topicNameGen, topicNameGen, Gen.anyString, Gen.anyString) {
          (inputTopic, outputTopic, inputKey, inputValue) =>
            for {
              topology <- ZStreamsBuilder { builder =>
                for {
                  sourceStream <- builder.stream[String, String](inputTopic)
                  sinkStream   <- sourceStream.mapValue(_.toUpperCase)
                  _            <- sinkStream.to(outputTopic)
                } yield ()
              }.provideSomeLayer(ZTestTopology.testConfigLayer)
              (_, outputValue) <- testTopology(inputTopic, outputTopic, inputKey, inputValue)
                .provideSomeLayer[TestEnvironment](makeTestLayer(topology))
            } yield assert(outputValue)(equalTo(inputValue.toUpperCase))
        }
      }
    )
}
