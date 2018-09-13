#https://github.com/anapsix/docker-alpine-java
FROM anapsix/alpine-java:8_jdk

ADD build/distributions/*.tar /usr/

ARG JENKINS_HOME=/var/lib/jenkins

RUN set -ex; \
adduser -u 100 -G nogroup -h ${JENKINS_HOME} -S jenkins && \
apk add --no-cache git rsync openssh && \
mkdir -p /var/cache/jenkins && \
chown -R jenkins: /usr/lib/jenkins /usr/distribution-scripts /var/cache/jenkins && \
cp /usr/distribution-scripts/docker/run.sh /run.sh

EXPOSE 8080/tcp

USER jenkins
WORKDIR ${JENKINS_HOME}
ENV JAVA_HOME="/opt/jdk"
CMD /run.sh
