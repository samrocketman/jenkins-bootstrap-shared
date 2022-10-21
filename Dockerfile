ARG base=alpine
FROM ${base}

ADD build/distributions/*.tar /usr/

ARG JENKINS_HOME=/var/lib/jenkins

RUN set -ex; \
adduser -u 100 -G nogroup -h ${JENKINS_HOME} -S jenkins && \
apk add --no-cache git rsync openssh openjdk11-jdk && \
mkdir -p /var/cache/jenkins ${JENKINS_HOME} && \
chown -R jenkins: /usr/lib/jenkins /var/cache/jenkins ${JENKINS_HOME} && \
ln -s /usr/lib/jenkins/distrib/daemon/run.sh /run.sh

EXPOSE 8080/tcp

USER jenkins
WORKDIR ${JENKINS_HOME}
ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk"
CMD /run.sh
