#!/usr/bin/env bash

TRANSFORMATION_JAR=`echo target/progression-domain-transformations-for-*-CC-SNAPSHOT.jar`
EVENT_TOOL_VERSION=5.2.1
EVENT_TOOL_JAR=target/event-tool-${EVENT_TOOL_VERSION}-swarm.jar

PROCESS_FILE=target/processFile
STANDALONE_XML=src/test/resources/standalone-ds.xml

echo TRANSFORMATION_JAR=${TRANSFORMATION_JAR}

[[ ! -f ${TRANSFORMATION_JAR} ]] && echo "File not found: "${TRANSFORMATION_JAR} && exit

# Download Event Tool JAR
[[ ! -f ${EVENT_TOOL_JAR} ]] && \
    curl https://libraries.mdv.cpp.nonlive/artifactory/repocentral/uk/gov/justice/event-tool/${EVENT_TOOL_VERSION}/event-tool-${EVENT_TOOL_VERSION}-swarm.jar -k > ${EVENT_TOOL_JAR}

touch ${PROCESS_FILE}

java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Dorg.wildfly.swarm.mainProcessFile=${PROCESS_FILE} \
   -DstreamCountReportingInterval=3 -jar -Devent.transformation.jar=${TRANSFORMATION_JAR} ${EVENT_TOOL_JAR} \
   -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
   -c ${STANDALONE_XML} -Dswarm.http.port=18080 -Dswarm.https.port=18443 -Dswarm.deployment.timeout=3600 \
   | tee target/transformation.log