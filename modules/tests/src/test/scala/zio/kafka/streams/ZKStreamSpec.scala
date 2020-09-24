package zio.kafka.streams

import zio.kafka.streams.testkit.ZTestTopology
import zio.kafka.streams.testkit.ZTestTopology._
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

object ZKStreamSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ZKStreamSpec")(
      testM("map") {
        checkM(topicNameGen, topicNameGen, Gen.anyString, Gen.anyString) {
          (inputTopic, outputTopic, inputKey, inputValue) =>

            val topology = ZStreamsBuilder { builder =>
              for {
                sourceStream <- builder.stream[String, String](inputTopic)
                sinkStream   <- sourceStream.map((key, value) => key.toUpperCase -> value.toUpperCase)
                _            <- sinkStream.to(outputTopic)
              } yield ()
            }.provideLayer(ZTestTopology.testConfigLayer())

            // anonymous function required or it won't compile
            testSingleMessage(inputTopic, outputTopic, inputKey, inputValue)
              .map { case (outputKey, outputValue) =>
                assert(outputKey)(equalTo(inputKey.toUpperCase)) &&
                  assert(outputValue)(equalTo(inputValue.toUpperCase))
              }
              .provideSomeLayer[TestEnvironment](testLayer(topology))
        }
      },
      testM("mapKey") {
        checkM(topicNameGen, topicNameGen, Gen.anyString, Gen.anyString) {
          (inputTopic, outputTopic, inputKey, inputValue) =>

            val topology = ZStreamsBuilder { builder =>
              for {
                sourceStream <- builder.stream[String, String](inputTopic)
                sinkStream   <- sourceStream.mapKey(_.toUpperCase)
                _            <- sinkStream.to(outputTopic)
              } yield ()
            }.provideLayer(ZTestTopology.testConfigLayer())

            testSingleMessage(inputTopic, outputTopic, inputKey, inputValue)
              .map { case (outputKey, _) =>
                assert(outputKey)(equalTo(inputKey.toUpperCase))
              }
              .provideSomeLayer[TestEnvironment](testLayer(topology))
        }
      },
      testM("mapValue") {
        checkM(topicNameGen, topicNameGen, Gen.anyString, Gen.anyString) {
          (inputTopic, outputTopic, inputKey, inputValue) =>

            val topology = ZStreamsBuilder { builder =>
              for {
                sourceStream <- builder.stream[String, String](inputTopic)
                sinkStream   <- sourceStream.mapValue(_.toUpperCase)
                _            <- sinkStream.to(outputTopic)
              } yield ()
            }.provideLayer(ZTestTopology.testConfigLayer())

            testSingleMessage(inputTopic, outputTopic, inputKey, inputValue)
              .map { case (_, outputValue) =>
                assert(outputValue)(equalTo(inputValue.toUpperCase))
              }
              .provideSomeLayer[TestEnvironment](testLayer(topology))
        }
      }
    )
}
