package zio.kafka

import zio.Has
import zio.config._

package object streams {

  final type KafkaStreamsEnv[T] = ZConfig[T] with Has[KafkaStreamsTopology.Service]
}
