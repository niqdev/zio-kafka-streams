package zio.kafka

import zio.logging._

package object streams {
  final type ZKSTopology = ZKSTopology.ZKSTopology
  final type Settings    = Settings.Settings

  final type TopologyEnv     = Logging with Settings
  final type KafkaStreamsEnv = TopologyEnv with ZKSTopology
}
