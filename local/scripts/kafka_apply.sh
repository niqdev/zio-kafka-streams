#!/bin/bash

CURRENT_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd -P)
cd ${CURRENT_PATH}

##############################

PARAM_ACTION=${1:?"Missing ACTION"}

BOOTSTRAP_SERVER="kafka:9092"

##############################

function kafka_exec {
  local COMMAND=$1
  docker exec -i local-kafka $COMMAND
}

##############################

echo "[+] kafka_apply"

echo "[*] ACTION=${PARAM_ACTION}"

case ${PARAM_ACTION} in
  "topic-list")
    kafka_exec "kafka-topics \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --list"
  ;;
  "topic-describe")
    PARAM_TOPIC_NAME=${2:?"Missing TOPIC_NAME"}
    echo "[*] TOPIC_NAME=${PARAM_TOPIC_NAME}"

    kafka_exec "kafka-topics \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --describe \
      --topic ${PARAM_TOPIC_NAME}"
  ;;
  "topic-create")
    PARAM_TOPIC_NAME=${2:?"Missing TOPIC_NAME"}
    echo "[*] TOPIC_NAME=${PARAM_TOPIC_NAME}"

    # convention <MESSAGE_TYPE>.<DATASET_NAME>.<DATA_NAME>
    kafka_exec "kafka-topics \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --create \
      --replication-factor 1 \
      --partitions 1 \
      --topic ${PARAM_TOPIC_NAME}"
  ;;
  "topic-delete")
    PARAM_TOPIC_NAME=${2:?"Missing TOPIC_NAME"}
    echo "[*] TOPIC_NAME=${PARAM_TOPIC_NAME}"

    kafka_exec "kafka-topics \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --delete \
      --topic ${PARAM_TOPIC_NAME}"
  ;;
  "topic-offset")
    PARAM_TOPIC_NAME=${2:?"Missing TOPIC_NAME"}
    echo "[*] TOPIC_NAME=${PARAM_TOPIC_NAME}"

    # view topic offset
    kafka_exec "kafka-run-class kafka.tools.GetOffsetShell \
      --broker-list ${BOOTSTRAP_SERVER} \
      --time -1 \
      --topic ${PARAM_TOPIC_NAME}"
  ;;
  # TODO not used
  "group-list")
    kafka_exec "kafka-consumer-groups \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --list"
  ;;
  # TODO not used
  "group-offset")
    PARAM_GROUP_NAME=${2:?"Missing GROUP_NAME"}
    echo "[*] GROUP_NAME=${PARAM_GROUP_NAME}"

    # view consumer group offset
    kafka_exec "kafka-consumer-groups \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --describe \
      --group ${PARAM_GROUP_NAME}"
  ;;
  # TODO not used
  "group-offset-reset")
    PARAM_GROUP_NAME=${2:?"Missing GROUP_NAME"}
    PARAM_TOPIC_NAME=${3:?"Missing TOPIC_NAME"}
    echo "[*] GROUP_NAME=${PARAM_GROUP_NAME}"
    echo "[*] TOPIC_NAME=${PARAM_TOPIC_NAME}"

    # TODO reset consumer group offset
    kafka_exec "kafka-consumer-groups \
      --bootstrap-server ${BOOTSTRAP_SERVER} \
      --group ${PARAM_GROUP_NAME} \
      --topic ${PARAM_TOPIC_NAME} \
      --reset-offsets \
      --to-earliest \
      --execute"
  ;;
  # TODO not used
  "schema-register")
    PARAM_SCHEMA_NAME=${2:?"Missing SCHEMA_NAME"}
    echo "[*] SCHEMA_NAME=${PARAM_SCHEMA_NAME}"
    LOCAL_SCHEMA_PATH="../schema"
    CONTAINER_SCHEMA_PATH="/schema"

    # prepare json request: wrap into schema object
    jq -n -c --arg schema "$(cat ${LOCAL_SCHEMA_PATH}/${PARAM_SCHEMA_NAME}.avsc)" '{schema: $schema}' \
      > "${LOCAL_SCHEMA_PATH}/${PARAM_SCHEMA_NAME}.json"

    # register schema
    docker exec -i local-schema-registry \
      curl -s -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      --data @"${CONTAINER_SCHEMA_PATH}/${PARAM_SCHEMA_NAME}.json" \
      "http://schema-registry:8081/subjects/${PARAM_SCHEMA_NAME}/versions"
  ;;
  *)
    echo "ERROR: unknown command"
    exit 1
  ;;
esac

echo "[-] kafka_apply"
