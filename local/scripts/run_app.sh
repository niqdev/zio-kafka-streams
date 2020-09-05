#!/bin/bash

export LOG_LEVEL="INFO"
export APPLICATION_ID="example-v0.1.0"
export BOOTSTRAP_SERVERS="localhost:9092"
export SCHEMA_REGISTRY_URL="http://localhost:8081"

CURRENT_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd -P)
cd ${CURRENT_PATH}/../..

sbt -jvm-debug 5005 "examples/runMain com.github.niqdev.Example"
