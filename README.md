# zio-kafka-streams

Write Kafka Streams applications using ZIO and access the internal state store directly via GraphQL

> WIP

TODO
* [ ] local kafka setup
* [ ] examples
* [ ] avro serde with confluent schema-registry integration
* [ ] core: runtime / ZKStream / ZKTable wrappers
* [ ] api with Caliban
* [ ] metrics with Prometheus
* [ ] testkit

## Development

```bash
# start kafka
make local-up

# run app locally
make local-run

# stop kafka
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

## Resources

* [Kafka Developer Guide](https://docs.confluent.io/current/streams/developer-guide/index.html)
* [Kafka Streams Interactive Queries](https://docs.confluent.io/current/streams/developer-guide/interactive-queries.html)
