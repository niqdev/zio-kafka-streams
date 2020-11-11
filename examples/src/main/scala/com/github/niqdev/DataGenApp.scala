package com.github.niqdev

import kafka.datagen._
import zio._
import zio.duration._
import zio.test.Sized

final case class MyKey(
  uuid: java.util.UUID
)
final case class MyValue(
  myBoolean: Boolean,
  myInt: Int,
  myString: String
)

object DataGenApp extends App {

  val producerSettingsLayer = KafkaGen
    .avroProducerSettingsLayer[MyKey, MyValue]("localhost:9092", "http://localhost:8081")

  // LOG_LEVEL=INFO sbt "examples/runMain com.github.niqdev.DataGenApp"
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .repeat(Schedule.recurs(3) && Schedule.spaced(1.second))
      .provideCustomLayer(Sized.live(1000) ++ producerSettingsLayer)
      .exitCode
}
