package com.github.niqdev

import kafka.datagen._
import zio._
import zio.duration._
import zio.kafka.producer.Producer

final case class MyKey(
  uuid: java.util.UUID
)
final case class MyValue(
  myBoolean: Boolean,
  myInt: Int,
  myString: String
)

// LOG_LEVEL=INFO sbt "examples/runMain com.github.niqdev.SimpleKafkaGenApp"
object SimpleKafkaGenApp extends KafkaGenApp(SimpleKafkaGen.producerSettingsLayer) {
  override def produce: RIO[ZEnv with Producer[Any, MyKey, MyValue], Unit] =
    SimpleKafkaGen.repeatWithDelay
}

object SimpleKafkaGen {

  val producerSettingsLayer: TaskLayer[Producer[Any, MyKey, MyValue]] =
    KafkaGen.avroProducerSettingsLayer[MyKey, MyValue]("localhost:9092", "http://localhost:8081")

  val singleElement: RIO[ZEnv with Producer[Any, MyKey, MyValue], Unit] =
    KafkaGen.produceAvro[MyKey, MyValue]("myTopic")

  val repeatWithDelay: RIO[ZEnv with Producer[Any, MyKey, MyValue], Unit] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .repeat(Schedule.recurs(3) && Schedule.spaced(1.second))
      .map(_ => ())

  val forever: RIO[ZEnv with Producer[Any, MyKey, MyValue], Unit] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .forever
      .map(_ => ())

  val foreverWithDelay: RIO[ZEnv with Producer[Any, MyKey, MyValue], Unit] =
    KafkaGen
      .produceAvro[MyKey, MyValue]("myTopic")
      .schedule(Schedule.spaced(5.second))
      .forever
      .map(_ => ())
}
