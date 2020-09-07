package zio.kafka

import zio._
import zio.config._
import zio.logging._

package object streams {

  final type KafkaStreamsEnv[T <: KafkaStreamsSettings] = Logging
    with ZConfig[T]
    with Has[KafkaStreamsTopology.Service]
}
