IMAGE_VERSION := $(shell awk 'BEGIN {FS="="}; $$1 == "version" { print $$2; exit }' gradle.properties )
PACKAGENAME := $(shell awk $$'$$1 == "PACKAGENAME:" { gsub("[\',]", "", $$2); print $$2 }' variables.gradle )
NEXUS_ENDPOINT :=
NEXUS_URL := https://$(NEXUS_ENDPOINT)/
DOCKER_PATH := samrocketman/jenkins
NEXUS_MANIFESTS := $(NEXUS_URL)repository/hosted-docker/v2/$(DOCKER_PATH)/manifests
.PHONE: help docker publish tag-release

help:
	@echo 'Run "make docker" to build docker images.'
	@echo 'Run "make tag-release" to idempotently git tag and publish to GitHub.'
	@echo 'Run "make publish" to publish docker image to Nexus. (includes "make docker" and "make tag-release")'

build/distributions/jenkins-ng-$(IMAGE_VERSION).tar: dependencies.gradle custom-plugins.txt gradle.properties
	./gradlew clean buildTar

docker: build/distributions/$(PACKAGENAME)-$(IMAGE_VERSION).tar
	docker build -t $(NEXUS_ENDPOINT)/$(DOCKER_PATH):$(IMAGE_VERSION) -f ./jenkins-bootstrap-shared/Dockerfile .

clean:
	docker rmi $(NEXUS_ENDPOINT)/$(DOCKER_PATH):$(IMAGE_VERSION)
	docker image prune -f

tag-release:
	set -ex; \
	if ! git tag | grep '\b$(IMAGE_VERSION)\b'; then \
		git tag $(IMAGE_VERSION); \
		git remote | xargs -n1 -I{} git push {} 'refs/tags/$(IMAGE_VERSION):refs/tags/$(IMAGE_VERSION)'; \
	elif git tag --points-at HEAD | grep '\b$(IMAGE_VERSION)\b'; then \
		echo 'SUCCESS: tag $(IMAGE_VERSION) already exists; nothing to do...'; \
	else \
		echo 'ERROR: tag $(IMAGE_VERSION) exists but does not point to this commit.  Cancelling release...'; \
		exit 1; \
	fi

nexus-health-check:
	curl --show-error -fsLIo /dev/null -- $(NEXUS_URL)

publish: tag-release docker nexus-health-check
	set -ex; \
	if curl --show-error -fsLo /dev/null -- $(NEXUS_MANIFESTS)/$(IMAGE_VERSION); then \
		echo "Docker image already exists in Nexus... not publishing again."; \
	else \
		echo "Docker image does not exist on Nexus so proceeding to publish to Nexus."; \
		docker push $(NEXUS_ENDPOINT)/$(DOCKER_PATH):$(IMAGE_VERSION); \
	fi
