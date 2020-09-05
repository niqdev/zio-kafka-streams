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
