# zio-kafka-streams

Write [Kafka Streams](https://docs.confluent.io/current/streams/developer-guide/index.html) applications using [ZIO](https://zio.dev) and access the internal state store directly via [GraphQL](https://ghostdogpr.github.io/caliban)

> WIP

* [Examples](#examples)
* [Development](#development)
* [TODO](#todo)

## Examples

### ToUpperCase

Probably the simplest Kafka Streams application you could think of
```scala
object ToUpperCaseTopology {
  // build the topology
  private[this] lazy val topology: RIO[KafkaStreamsConfig with CustomConfig, Topology] =
    for {
      sourceTopic <- CustomConfig.sourceTopic
      sinkTopic   <- CustomConfig.sinkTopic
      topology    <- ZStreamsBuilder { builder =>
        for {
          // compose the topology using ZKStream and ZKTable
          sourceStream <- builder.stream[String, String](sourceTopic)
          sinkStream   <- sourceStream.mapValue(_.toUpperCase)
          _            <- sinkStream.to(sinkTopic)
        } yield ()
      }
    } yield topology
  // define the topology's layer
  val layer: RLayer[ZEnv, KafkaStreamsTopology with KafkaStreamsConfig] =
    ToUpperCaseConfig.layer >+> KafkaStreamsTopology.make(topology)
}
// setup runtime
object ToUpperCaseApp extends KafkaStreamsApp(ToUpperCaseTopology.layer)
```

How to run the example
```bash
# start kafka
make local-up

# create source topic
make topic-create name=example.source.v1

# start application
LOG_LEVEL="INFO" sbt "examples/runMain com.github.niqdev.ToUpperCaseApp"

# access kafka
docker exec -it local-kafka bash

# publish messages
kafka-console-producer --bootstrap-server kafka:9092 --topic example.source.v1

# consume messages
kafka-console-consumer --bootstrap-server kafka:9092 --topic example.sink.v1
```

Complete example of [ToUpperCaseApp](https://github.com/niqdev/zio-kafka-streams/blob/master/examples/src/main/scala/com/github/niqdev/ToUpperCaseApp.scala)

### GitHubApp

Joining avro streams has never been so easy ;-)
```scala
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
    Logging.console() ++ GitHubConfig.localLayer >+> KafkaStreamsTopology.make(topology)
}
object GitHubApp extends KafkaStreamsApp(GitHubTopology.layer)
```

How to run the example
```bash
# start kafka
make local-up

# create source topics
make topic-create name=example.user.v1
make topic-create name=example.repository.v1

# start application
make local-run

# TODO
* generate schema
* register schema
* format data
* publish data
```

Complete example of [GitHubApp](https://github.com/niqdev/zio-kafka-streams/blob/master/examples/src/main/scala/com/github/niqdev/GitHubApp.scala)

## Development

```bash
# start containers in background
# zookeeper|kafka|kafka-rest|kafka-ui|schema-registry|schema-registry-ui
make local-up

# stop all containers
make local-down

# cli
make topic-list
make topic-describe name=<TOPIC_NAME>
make topic-create name=<TOPIC_NAME>
make topic-delete name=<TOPIC_NAME>
make topic-offset name=<TOPIC_NAME>

# [mac|linux] kafka ui
[open|xdg-open] http://localhost:8000
# [mac|linux] schema-registry ui
[open|xdg-open] http://localhost:8001
```

## TODO

* [ ] kafkacat docker
* [ ] local kafka setup
* [ ] examples
* [ ] avro serde with confluent schema-registry
* [ ] json serde with circe/zio-json
* [ ] core wrappers
* [ ] interop-cats
* [ ] api with Caliban (pagination + subscriptions)
    - [Kafka Streams Interactive Queries](https://docs.confluent.io/current/streams/developer-guide/interactive-queries.html)
* [ ] metrics with Prometheus
* [ ] testkit
* [ ] helm chart StatefulSet
* [ ] test + documentation
