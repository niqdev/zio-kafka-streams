package com.github.niqdev

import kafka.datagen._
import zio._
import zio.duration._
import zio.kafka.producer.Producer
import zio.test.Sized

final case class MyKey(
  uuid: java.util.UUID
)
final case class MyValue(
  myBoolean: Boolean,
  myInt: Int,
  myString: String
)

// TODO hide zio.test.Sized
// TODO use KafkaGenApp
object DataGenApp extends App {

  val producerSettingsLayer =
    AvroProducerSettings("localhost:9092", "http://localhost:8081").layer[MyKey, MyValue]

  val singleElement: RIO[ZEnv with Producer[Any, MyKey, MyValue] with Sized, Unit] =
    KafkaGen.produceAvro[MyKey, MyValue]("myTopic")

  val repeatWithDelay: RIO[ZEnv with Producer[Any, MyKey, MyValue] with Sized, Unit] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .repeat(Schedule.recurs(3) && Schedule.spaced(1.second))
      .map(_ => ())

  val forever: RIO[ZEnv with Producer[Any, MyKey, MyValue] with Sized, Unit] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .forever
      .map(_ => ())

  val foreverWithDelay: RIO[ZEnv with Producer[Any, MyKey, MyValue] with Sized, Unit] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .schedule(Schedule.spaced(5.second))
      .forever
      .map(_ => ())

  // LOG_LEVEL=INFO sbt "examples/runMain com.github.niqdev.DataGenApp"
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    repeatWithDelay.provideCustomLayer(producerSettingsLayer ++ Sized.live(1000)).exitCode
}
