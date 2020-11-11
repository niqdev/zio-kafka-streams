package kafka.datagen

import com.sksamuel.avro4s.{ Decoder, Encoder }
import kafka.streams.serde.AvroCodec
import org.apache.kafka.clients.producer.ProducerRecord
import zio._
import zio.console.putStrLn
import zio.kafka.producer.{ Producer, ProducerSettings }
import zio.kafka.serde.Serializer
import zio.test.Sized
import zio.test.magnolia.DeriveGen

// https://github.com/zio/zio/blob/master/test-magnolia-tests/shared/src/test/scala/zio/test/magnolia/DeriveGenSpec.scala
object KafkaGen {

  def avroProducerSettingsLayer[K >: Null: Encoder: Decoder: Tag, V >: Null: Encoder: Decoder: Tag](
    bootstrapServer: String,
    schemaRegistryUrl: String
  ) =
    Producer
      .make(
        ProducerSettings(List(bootstrapServer)),
        Serializer(AvroCodec.genericKey[K].serde(schemaRegistryUrl).serializer()),
        Serializer(AvroCodec.genericValue[V].serde(schemaRegistryUrl).serializer())
      )
      .toLayer

  def produceAvro[K: Tag: DeriveGen, V: Tag: DeriveGen](
    topic: String
  ): RIO[ZEnv with Producer[Any, K, V] with Sized, Unit] = {
    val sampleKey   = DeriveGen[K].sample.map(_.value)
    val sampleValue = DeriveGen[V].sample.map(_.value)
    val sample      = sampleKey.zip(sampleValue)

    sample
      .map { case (key, value) =>
        Producer
          .produce[Any, K, V](
            new ProducerRecord(topic, key, value)
          )
          .map(recordMedata => (key, value, recordMedata))
      }
      .foreach(_.flatMap { case (key, value, recordMedata) =>
        putStrLn(s"""
          |Produced Avro:
          |key=$key
          |value=$value
          |topic=$topic
          |partition=${recordMedata.partition}
          |offset=${recordMedata.offset}
          |timestamp=${recordMedata.timestamp}
          |""".stripMargin)
      })
  }
}
