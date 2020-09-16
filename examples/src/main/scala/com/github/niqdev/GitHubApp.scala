package com.github.niqdev

import com.github.niqdev.GitHubConfig.CustomConfig
import com.github.niqdev.schema.GitHubEvent._
import com.github.niqdev.schema.repository._
import com.github.niqdev.schema.user._
import org.apache.kafka.streams.Topology
import zio._
import zio.config.ConfigDescriptor.string
import zio.config._
import zio.kafka.streams._
import zio.logging._

/*
 * Kafka Streams application
 */
object GitHubApp extends KafkaStreamsApp(GitHubTopology.layer)

/*
 * Topology
 */
object GitHubTopology {
  private[this] lazy val topology: RIO[KafkaStreamsConfig with CustomConfig with Logging, Topology] =
    for {
      config <- KafkaStreamsConfig.config
      _      <- log.info(s"Running ${config.applicationId}")
      _      <- CustomConfig.prettyPrint.flatMap(values => log.info(values))
      topics <- CustomConfig.topics
      topology <- ZStreamsBuilder { builder =>
        for {
          userStream         <- builder.streamAvro[UserKey, UserValue](topics.userSource)
          repositoryStream   <- builder.streamAvro[RepositoryKey, RepositoryValue](topics.repositorySource)
          ghUserStream       <- userStream.mapKey(GitHubEventKey.fromUser)
          ghRepositoryStream <- repositoryStream.mapKey(GitHubEventKey.fromRepository)
          ghUserTable        <- ghUserStream.toTableAvro
          ghRepositoryTable  <- ghRepositoryStream.toTableAvro
          gitHubTable        <- ghUserTable.joinAvro(ghRepositoryTable)(GitHubEventValue.joinUserRepository)
          gitHubStream       <- gitHubTable.toStream
          _                  <- gitHubStream.toAvro(topics.gitHubSink)
        } yield ()
      }
    } yield topology

  val layer: RLayer[ZEnv, KafkaStreamsTopology with KafkaStreamsConfig] =
    Logging.console() ++ GitHubConfig.envLayer >+> KafkaStreamsTopology.make(topology)
}

/*
 * Configurations
 */
// TODO update example with refined/newtype
final case class GitHubConfig(
  applicationId: String,
  bootstrapServers: String,
  schemaRegistryUrl: String,
  logLevel: String,
  topics: GitHubTopics
)
final case class GitHubTopics(
  userSource: String,
  repositorySource: String,
  gitHubSink: String
)
object GitHubConfig {
  type CustomConfig = Has[CustomConfig.Service]

  object CustomConfig {
    trait Service {
      def topics: Task[GitHubTopics]
      def prettyPrint: Task[String]
    }

    def topics: RIO[CustomConfig, GitHubTopics] =
      ZIO.accessM[CustomConfig](_.get.topics)
    def prettyPrint: RIO[CustomConfig, String] =
      ZIO.accessM[CustomConfig](_.get.prettyPrint)
  }

  private[this] lazy val configDescriptor: ConfigDescriptor[GitHubConfig] = {
    val topicsConfig =
      (string("USER_SOURCE") |@|
        string("REPOSITORY_SOURCE") |@|
        string("GITHUB_SINK"))(
        GitHubTopics.apply,
        GitHubTopics.unapply
      )

    (string("APPLICATION_ID") |@|
      string("BOOTSTRAP_SERVERS") |@|
      string("SCHEMA_REGISTRY_URL") |@|
      string("LOG_LEVEL") |@|
      topicsConfig)(
      GitHubConfig.apply,
      GitHubConfig.unapply
    )
  }

  private[this] lazy val toAppConfig: GitHubConfig => AppConfig =
    config =>
      AppConfig(
        applicationId = config.applicationId,
        bootstrapServers = config.bootstrapServers,
        schemaRegistryUrl = Some(config.schemaRegistryUrl),
        debug = true
      )

  /*
   * Local configurations
   */
  private[this] lazy val localConfig: IO[ReadError[String], GitHubConfig] = {
    val configMap =
      Map(
        "APPLICATION_ID"      -> "github-local-v0.1.0",
        "BOOTSTRAP_SERVERS"   -> "localhost:9092",
        "SCHEMA_REGISTRY_URL" -> "http://localhost:8081",
        "LOG_LEVEL"           -> "???",
        "USER_SOURCE"         -> "example.user.v1",
        "REPOSITORY_SOURCE"   -> "example.repository.v1",
        "GITHUB_SINK"         -> "example.github.v1"
      )
    ZIO.fromEither(read(configDescriptor from ConfigSource.fromMap(configMap)))
  }
  private[this] lazy val localConfigLayer: ULayer[KafkaStreamsConfig] =
    KafkaStreamsConfig.make(localConfig.map(toAppConfig))
  private[this] lazy val localCustomConfigLayer =
    ZLayer.succeed(new CustomConfig.Service {
      override def topics: Task[GitHubTopics] =
        localConfig.map(_.topics)
      override def prettyPrint: Task[String] =
        localConfig
          .flatMap(values => UIO(s"""
               |LOCAL custom configurations
               |LOG_LEVEL: ${values.logLevel}
               |USER_SOURCE: ${values.topics.userSource}
               |REPOSITORY_SOURCE: ${values.topics.repositorySource}
               |GITHUB_SINK: ${values.topics.gitHubSink}
               |""".stripMargin))
          .absorb
    })

  /*
   * Env configurations
   */
  private[this] lazy val envConfig =
    ConfigSource
      .fromSystemEnv
      .flatMap(configSource => ZIO.fromEither(read(configDescriptor from configSource)))
  private[this] lazy val envConfigLayer =
    KafkaStreamsConfig.make(envConfig.provideLayer(zio.system.System.live).map(toAppConfig))
  private[this] lazy val envCustomConfigLayer =
    ZLayer.succeed(new CustomConfig.Service {
      override def topics: Task[GitHubTopics] =
        envConfig.provideLayer(zio.system.System.live).map(_.topics)
      override def prettyPrint: Task[String] =
        envConfig
          .provideLayer(zio.system.System.live)
          .flatMap(values => UIO(s"""
               |ENV custom configurations
               |LOG_LEVEL: ${values.logLevel}
               |USER_SOURCE: ${values.topics.userSource}
               |REPOSITORY_SOURCE: ${values.topics.repositorySource}
               |GITHUB_SINK: ${values.topics.gitHubSink}
               |""".stripMargin))
          .absorb
    })

  lazy val localLayer = localConfigLayer ++ localCustomConfigLayer
  lazy val envLayer   = envConfigLayer ++ envCustomConfigLayer
}
