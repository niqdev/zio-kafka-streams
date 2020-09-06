package zio.kafka

import zio.Has
import zio.config.ZConfig
import zio.kafka.streams.settings.AppSettings
import zio.logging.Logging

package object streams {

  final type KafkaStreamsEnv[T] = Logging
    with ZConfig[AppSettings]
    with ZConfig[T]
    with Has[KafkaStreamsTopology.Service]
}
