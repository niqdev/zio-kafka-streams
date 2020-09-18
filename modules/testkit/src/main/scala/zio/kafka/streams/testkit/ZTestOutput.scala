package zio.kafka.streams
package testkit

import org.apache.kafka.streams.{ KeyValue, TestOutputTopic }
import zio._

import scala.jdk.CollectionConverters._

sealed abstract class ZTestOutput[K, V](private val topic: TestOutputTopic[K, V]) {

  private[this] val toKeyValue: KeyValue[K, V] => (K, V) =
    keyValue => keyValue.key -> keyValue.value

  def consume: Task[(K, V)] =
    Task.effect(topic.readKeyValue()).map(toKeyValue)

  def consumeAll: Task[List[(K, V)]] =
    Task.effect(topic.readKeyValuesToList()).map(_.asScala.toList.map(toKeyValue))

  def consumeValue: Task[V] =
    Task.effect(topic.readValue())

  def isEmpty: Task[Boolean] =
    Task.effect(topic.isEmpty)

  def size: Task[Long] =
    Task.effect(topic.getQueueSize)
}
object ZTestOutput {

  def apply[K, V](topic: TestOutputTopic[K, V]): Task[ZTestOutput[K, V]] =
    Task.effect(new ZTestOutput[K, V](topic) {})
}
