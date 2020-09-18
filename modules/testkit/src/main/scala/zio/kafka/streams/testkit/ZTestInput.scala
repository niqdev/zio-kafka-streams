package zio.kafka.streams
package testkit

import org.apache.kafka.streams.TestInputTopic
import zio._

sealed abstract class ZTestInput[K, V](private val topic: TestInputTopic[K, V]) {

  def produce(key: K, value: V): Task[Unit] =
    Task.effect(topic.pipeInput(key, value))

  def produceAll(values: List[(K, V)]): Task[Long] =
    Task.effect {
      values.foldLeft(0L) { (count, keyValue) =>
        topic.pipeInput(keyValue._1, keyValue._2)
        count + 1
      }
    }

  // TODO add warnings: it fails if a not-null key is required e.g. avro message
  def produceValue(value: V): Task[Unit] =
    Task.effect(topic.pipeInput(value))

  def produceValues(values: List[V]): Task[Long] =
    Task.effect {
      values.foldLeft(0L) { (count, value) =>
        topic.pipeInput(value)
        count + 1
      }
    }
}
object ZTestInput {

  def apply[K, V](topic: TestInputTopic[K, V]): Task[ZTestInput[K, V]] =
    Task.effect(new ZTestInput[K, V](topic) {})
}
