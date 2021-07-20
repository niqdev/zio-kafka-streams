require-%:
	@ if [ "$(shell command -v ${*} 2> /dev/null)" = "" ]; then \
		echo "[$*] not found"; \
		exit 1; \
	fi

check-param-%:
	@ if [ "${${*}}" = "" ]; then \
		echo "Missing parameter: [$*]"; \
		exit 1; \
	fi

##############################

.PHONY: local-up
local-up: require-docker
	docker-compose -f local/docker-compose.yml up -d

.PHONY: local-down
local-down: require-docker
	docker-compose -f local/docker-compose.yml down -v

.PHONY: local-run
local-run: require-sbt
	./local/scripts/run_app.sh

##############################

.PHONY: topic-list
topic-list: require-docker
	./local/scripts/kafka_apply.sh "topic-list"

.PHONY: topic-describe
topic-describe: require-docker check-param-name
	./local/scripts/kafka_apply.sh "topic-describe" ${name}

.PHONY: topic-create
topic-create: require-docker check-param-name
	./local/scripts/kafka_apply.sh "topic-create" ${name}

.PHONY: topic-delete
topic-delete: require-docker check-param-name
	./local/scripts/kafka_apply.sh "topic-delete" ${name}

.PHONY: topic-offset
topic-offset: require-docker check-param-name
	./local/scripts/kafka_apply.sh "topic-offset" ${name}

.PHONY: group-list
group-list: require-docker
	./local/scripts/kafka_apply.sh "group-list"

.PHONY: group-offset
group-offset: require-docker check-param-name
	./local/scripts/kafka_apply.sh "group-offset" ${name}

.PHONY: schema-generate
schema-generate: require-sbt
	rm -f ./local/schema/{*.avsc,*.json}
	sbt "examples/runMain com.github.niqdev.GenerateSchema"

.PHONY: schema-register
schema-register: require-docker require-jq check-param-name
	./local/scripts/kafka_apply.sh "schema-register" ${name}

.PHONY: produce-avro
produce-avro: require-docker check-param-schema-key-id check-param-schema-value-id check-param-topic-name check-param-event-name
	./local/scripts/kafka_apply.sh "produce-avro" ${schema-key-id} ${schema-value-id} ${topic-name} ${event-name}

.PHONY: produce-avro-value
produce-avro-value: require-docker check-param-schema-value-id check-param-topic-name check-param-event-name
	./local/scripts/kafka_apply.sh "produce-avro-value" ${schema-value-id} ${topic-name} ${event-name}

.PHONY: format-data
format-data: require-jq check-param-name
	./local/scripts/format_data.sh ${name}

##############################

.PHONY: topic-create-all
topic-create-all:
	@make topic-create name=example.user.v1
	@make topic-create name=example.repository.v1
	@make topic-list

.PHONY: schema-register-all
schema-register-all: schema-generate
	@make schema-register name=example.user.v1-key
	@make schema-register name=example.user.v1-value
	@make schema-register name=example.repository.v1-key
	@make schema-register name=example.repository.v1-value

.PHONY: format-data-all
format-data-all: require-jq
	@make format-data name=user
	@make format-data name=repository
