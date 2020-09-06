package zio.kafka

import zio.Has
import zio.config.ZConfig
import zio.logging.Logging

package object streams {

  final type KafkaStreamsEnv[T <: KafkaStreamsSettings] = Logging
    with ZConfig[T]
    with Has[KafkaStreamsTopology.Service]
}
