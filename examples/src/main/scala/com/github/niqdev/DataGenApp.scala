package com.github.niqdev

import kafka.streams.serde.AvroCodec
import org.apache.kafka.clients.producer.ProducerRecord
import zio._
import zio.console._
import zio.duration._
import zio.kafka.producer.{ Producer, ProducerSettings }
import zio.kafka.serde.Serializer
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{ Gen, Sized }

// https://github.com/zio/zio/blob/master/test-magnolia-tests/shared/src/test/scala/zio/test/magnolia/DeriveGenSpec.scala
object DataGenApp extends App {

  final case class MyKey(uuid: java.util.UUID)
  object MyKey {
    val genMyKey: Gen[Random with Sized, MyKey] =
      DeriveGen[MyKey]
    implicit val myKeyAvroCodec: AvroCodec[MyKey] =
      AvroCodec.genericKey[MyKey]
  }
  final case class MyValue(
    myBoolean: Boolean,
    myInt: Int,
    myString: String
  )
  object MyValue {
    val genMyValue: Gen[Random with Sized, MyValue] =
      DeriveGen[MyValue]
    implicit val myValueAvroCodec: AvroCodec[MyValue] =
      AvroCodec.genericValue[MyValue]
  }

  def produce: RIO[ZEnv, Unit] = {
    val myKeySerde   = AvroCodec[MyKey].serde("http://localhost:8081")
    val myValueSerde = AvroCodec[MyValue].serde("http://localhost:8081")

    MyKey
      .genMyKey
      .sample
      .zip(MyValue.genMyValue.sample)
      .map(sample =>
        Producer
          .produce[Any, MyKey, MyValue](
            new ProducerRecord("myTopic", sample._1.value, sample._2.value)
          )
      )
      .foreach(result =>
        result.flatMap(recordMedata => putStrLn(s">>>>>>>>>>>>>>>>>>>>${recordMedata.offset()}"))
      )
      .provideCustomLayer(
        Sized.live(1000) ++ Producer
          .make(
            ProducerSettings(List("localhost:9092")),
            Serializer(myKeySerde.serializer()),
            Serializer(myValueSerde.serializer())
          )
          .toLayer
      )
  }

  // sbt "examples/runMain com.github.niqdev.DataGenApp"
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    produce
      .repeat(Schedule.recurs(3) && Schedule.spaced(1.second))
      .exitCode
}
