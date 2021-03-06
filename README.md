# zio-kafka-streams

[![Build Status][build-image]][build-url]

[build-image]: https://travis-ci.org/niqdev/zio-kafka-streams.svg?branch=master
[build-url]: https://travis-ci.org/niqdev/zio-kafka-streams

Write and test [Kafka Streams](https://docs.confluent.io/current/streams/developer-guide/index.html) applications using [ZIO](https://zio.dev) ~~and expose the internal state store directly via [GraphQL](https://ghostdogpr.github.io/caliban)~~

Add the following lines to your `build.sbt`

```sbt
libraryDependencies ++= Seq(
  "???" %% "zio-kafka-streams" % "???", // core DONE, incomplete api and tests
  "???" %% "zio-kafka-streams-api" % "???", // TODO graphQL (?)
  "???" %% "zio-kafka-streams-testkit" % "???" % Test, // core DONE, incomplete api and tests
  "???" %% "kafka-streams-serde" % "???", // DONE, missing tests
  "???" %% "kafka-datagen" % "???" // TODO incomplete
)
```

:construction::construction::construction::construction::construction::construction::construction::construction::construction::construction:
:warning: **Work in Progress** :warning:
:construction::construction::construction::construction::construction::construction::construction::construction::construction::construction:

* [Examples](#examples)
* [TestKit](#testkit)
* [Serdes](#serdes)
* [DataGen](#datagen)
* [Development](#development)
* [TODO](#todo)

## Examples

Examples of how to write Kafka Streams applications using `ZStreamsBuilder`, `ZKStream` and `ZKTable`

### ToUpperCase

Probably the simplest Kafka Streams application you could think of
```scala
object ToUpperCaseTopology {
  // build the topology
  lazy val topology: RIO[KafkaStreamsConfig with CustomConfig, Topology] =
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

Joining Avro streams integrated with Schema Registry has never been so easy ;-)
```scala
object GitHubTopology {
  lazy val topology: RIO[KafkaStreamsConfig with CustomConfig with Logging, Topology] =
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
object GitHubApp extends KafkaStreamsApp(GitHubTopology.layer)
```

How to run the example
```bash
# start kafka
make local-up

# create source topics
make topic-create-all

# generate avsc and register schema
make schema-register-all

# start application
make local-run
```

How to publish messages locally
```bash
# format example data
make format-data-all

# access schema-registry
docker exec -it local-schema-registry bash

# export producer config (see below)
SCHEMA_KEY_ID=XXX
SCHEMA_VALUE_ID=YYY
TOPIC_NAME=ZZZ

# start avro producer
kafka-avro-console-producer \
  --bootstrap-server kafka:29092 \
  --property schema.registry.url="http://schema-registry:8081" \
  --property parse.key=true \
  --property key.schema="$(curl -s http://schema-registry:8081/schemas/ids/$SCHEMA_KEY_ID | jq -r .schema)" \
  --property value.schema="$(curl -s http://schema-registry:8081/schemas/ids/$SCHEMA_VALUE_ID | jq -r .schema)" \
  --property key.separator=::: \
  --topic $TOPIC_NAME
```

How to consume messages locally
```bash
# access schema-registry
docker exec -it local-schema-registry bash

# export consumer config (see below)
TOPIC_NAME=XYZ

# start avro consumer
kafka-avro-console-consumer \
  --bootstrap-server kafka:29092 \
  --property schema.registry.url="http://schema-registry:8081" \
  --property schema.id.separator=: \
  --property print.key=true \
  --property print.schema.ids=true \
  --property key.separator=, \
  --topic $TOPIC_NAME \
  --from-beginning \
  --max-messages 10
```

Configurations
```bash
# produce to "user" topic
SCHEMA_KEY_ID=1
SCHEMA_VALUE_ID=2
TOPIC_NAME=example.user.v1

# produce to "repository" topic
SCHEMA_KEY_ID=3
SCHEMA_VALUE_ID=4
TOPIC_NAME=example.repository.v1

# consume from topics
TOPIC_NAME=example.user.v1
TOPIC_NAME=example.repository.v1
TOPIC_NAME=example.github.v1
```

Complete example of [GitHubApp](https://github.com/niqdev/zio-kafka-streams/blob/master/examples/src/main/scala/com/github/niqdev/GitHubApp.scala)

## TestKit

How to test [ToUpperCaseTopology](https://github.com/niqdev/zio-kafka-streams/blob/master/examples/src/main/scala/com/github/niqdev/ToUpperCaseApp.scala) topology with `ZTestTopology`, `ZTestInput` and `ZTestOutput`
```scala
// LOG_LEVEL=WARN sbt "test:testOnly *ToUpperCaseSpec"

testM("topology") {
  for {
    sourceTopic <- CustomConfig.sourceTopic
    sinkTopic   <- CustomConfig.sinkTopic
    outputValue <- ZTestTopology.driver.use { driver =>
      for {
        input  <- driver.createInput[String, String](sourceTopic)
        output <- driver.createOutput[String, String](sinkTopic)
        _      <- input.produceValue("myValue")
        value  <- output.consumeValue
      } yield value
    }
  } yield assert(outputValue)(equalTo("MYVALUE"))
}.provideSomeLayerShared(testLayer)
```

More examples in the [tests](https://github.com/niqdev/zio-kafka-streams/tree/master/modules/tests/src/test/scala) module

## Serdes

`kafka-streams-serde` is an independent module without ZIO dependencies useful to build [Serdes](https://docs.confluent.io/current/streams/developer-guide/datatypes.html) with your favourite effect system

Example of how to autoderive Avro serde for keys and values integrated with Confluent [Schema Registry](https://docs.confluent.io/current/schema-registry/index.html) leveraging [avro4s](https://github.com/sksamuel/avro4s)
```scala
import kafka.streams.serde._

final case class DummyValue(string: String)
object DummyValue {
  final implicit val dummyValueAvroCodec: AvroCodec[DummyValue] =
    AvroCodec.genericValue[DummyValue]
}
```

For more examples with [refined](https://github.com/fthomas/refined), [newtype](https://github.com/estatico/scala-newtype), [enumeratum](https://github.com/lloydmeta/enumeratum) and custom types see the [schema](https://github.com/niqdev/zio-kafka-streams/tree/master/examples/src/main/scala/com/github/niqdev/schema) package

Example of how to build a syntax with [Cats Effect](https://typelevel.org/cats-effect) using `Record` and `AvroRecord`
```scala
object syntax {
  final implicit def streamsBuilderSyntax[F[_]](builder: StreamsBuilder): StreamsBuilderOps[F] =
    new StreamsBuilderOps(builder)
}

final class StreamsBuilderOps[F[_]](private val builder: StreamsBuilder) extends AnyVal {
  def streamF[K, V](
    topic: String,
    schemaRegistry: String
  )(implicit F: Sync[F], C: AvroRecordConsumed[K, V]): F[KStream[K, V]] =
    F.delay(builder.stream(topic)(C.consumed(schemaRegistry)))
}
```

Complete example of [KafkaStreamsCatsApp](https://github.com/niqdev/zio-kafka-streams/blob/master/examples/src/main/scala/com/github/niqdev/KafkaStreamsCatsApp.scala)

## DataGen

> TODO

```bash
LOG_LEVEL=INFO sbt "examples/runMain com.github.niqdev.SimpleKafkaGenApp"
```

## Development

```bash
# start containers in background
# zookeeper|kafka|kafka-rest|kafka-ui|schema-registry|schema-registry-ui|kowl
make local-up

# stop all containers
make local-down

# cli
make topic-list
make topic-describe name=<TOPIC_NAME>
make topic-create name=<TOPIC_NAME>
make topic-delete name=<TOPIC_NAME>
make topic-offset name=<TOPIC_NAME>
make group-list
make group-offset name=<GROUP_NAME>

# [mac|linux] kafka ui
[open|xdg-open] http://localhost:8000
# [mac|linux] schema-registry ui
[open|xdg-open] http://localhost:8001
# [mac|linux] kowl (another ui)
[open|xdg-open] http://localhost:8002
```

How to build an event manually

```bash
# example
EVENT_NAME=<my-event-name>

# stringify json event
cat local/data/$EVENT_NAME.json | jq -c | jq -R

# format message "<json_key>:::<json_value>"
echo "$(cat local/data/$EVENT_NAME-key.json | jq -c):::$(cat local/data/$EVENT_NAME-value.json | jq -c)" > local/data/$EVENT_NAME-event.txt

# produce sample message
make produce-avro \
  schema-key-id=1 \
  schema-value-id=2 \
  topic-name=<TOPIC_NAME> \
  event-name=$EVENT_NAME
```

How to interact with `schema-registry` apis

```bash
TOPIC_NAME=<???>

# verify registered schema
curl -s -X GET localhost:8081/subjects/$TOPIC_NAME-value/versions
curl -s -X GET localhost:8081/subjects/$TOPIC_NAME-value/versions/1 | jq
# extract avsc
curl -s -X GET localhost:8081/subjects/$TOPIC_NAME-value/versions/1 | jq -r ".schema" | jq
```

## TODO

* [ ] zio-prelude e.g. [newtype](https://github.com/zio/zio-prelude/blob/d34b5c0e74557edd8709f7c45b40297bf4280d77/src/main/scala/zio/prelude/NewtypeModule.scala) or refined ?
* [ ] kafkacat docker
    - `docker run --rm --name kafkacat edenhill/kafkacat:1.6.0`
* [ ] json serde with circe/zio-json + xml serde (?)
* [ ] interop-cats
* [ ] api with Caliban (pagination + subscriptions)
    - [Kafka Streams Interactive Queries](https://docs.confluent.io/current/streams/developer-guide/interactive-queries.html)
* [ ] metrics with Prometheus
* [ ] helm chart StatefulSet
* [ ] convert GenerateSchema into sbt plugin
* [ ] replace `kafka-streams-scala` with plain Java or zio-kafka?
* [ ] topology description with [eisner](https://github.com/laserdisc-io/eisner)
* [ ] Scala 3 compatibility

<!--

# TODO resources
https://docs.confluent.io/current/streams/developer-guide/test-streams.html
https://www.confluent.io/blog/testing-kafka-streams
https://www.confluent.io/blog/test-kafka-streams-with-topologytestdriver
https://www.confluent.io/blog/stream-processing-part-2-testing-your-streaming-application
https://medium.com/bakdata/fluent-kafka-streams-tests-e641785171ec
https://github.com/embeddedkafka/embedded-kafka-schema-registry
https://github.com/confluentinc/kafka-streams-examples/blob/5.5.1-post/src/test/java/io/confluent/examples/streams/GenericAvroIntegrationTest.java

-->
