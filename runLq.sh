#!/usr/bin/env bash

#The prerequisite for this script is that vagrant is running
#Script that runs, liquibase, deploys wars and runs integration tests


${VAGRANT_DIR:?"Please export VAGRANT_DIR environment variable to point at atcm-vagrant"}
WILDFLY_DEPLOYMENT_DIR="${VAGRANT_DIR}/deployments"
CONTEXT_NAME=progression
EVENT_LOG_VERSION=1.1.4
EVENT_BUFFER_VERSION=1.1.4



#fail script on error
set -e

function buildWars {
  echo
  echo "Building wars."
  mvn clean install -nsu -DskipTests
  echo "\n"
  echo "Finished building wars"
}

function startVagrant {
  echo "Starting Vagrant machine from " $VAGRANT_DIR
  export VAGRANT_CWD=$VAGRANT_DIR
  vagrant up;
}

function deleteAndDeployWars {
  echo
  echo "Deleting wars from $WILDFLY_DEPLOYMENT_DIR....."
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.war
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.deployed

  sleep 10

  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.undeployed
  find . \( -iname "${CONTEXT_NAME}-service-*.war" \) -exec cp {} $WILDFLY_DEPLOYMENT_DIR \;
  echo "Copied wars to $WILDFLY_DEPLOYMENT_DIR"
}


function healthCheck {
  CONTEXT=("$CONTEXT_NAME-command-api"  "$CONTEXT_NAME-command-handler" "${CONTEXT_NAME}-query-api" "${CONTEXT_NAME}-query-view" "${CONTEXT_NAME}-event-listener")
  CONTEXT_COUNT=${#CONTEXT[@]}
  TIMEOUT=90
  RETRY_DELAY=5
  START_TIME=$(date +%s)

  echo "Start time is $START_TIME"
  echo "Starting health check on ${CONTEXT[@]}"
  echo "Conducting health check on $CONTEXT_COUNT contexts"
  echo "TIMEOUT is $TIMEOUT Seconds"
  echo "RETRY_DELAY $RETRY_DELAY Seconds"

  while [ true ]
  do
      DEPLOYED=0

      for i in ${CONTEXT[@]}
      do
        CHECK_STRING="curl --connect-timeout 1 -s http://localhost:8080/$i/internal/metrics/ping"
        echo -n $CHECK_STRING
        CHECK=$( $CHECK_STRING )  >/dev/null 2>&1
        echo $CHECK | grep pong >/dev/null 2>&1 && DEPLOYED=$((DEPLOYED + 1))
        echo $CHECK | grep pong >/dev/null 2>&1 && echo " pong" || echo " DOWN"
      done

      echo
      echo RESULT:  ${DEPLOYED} out of  ${CONTEXT_COUNT} wars came back with pong

      [ "${DEPLOYED}" -eq "${CONTEXT_COUNT}" ] && break

      TIME_NOW=$(date +%s)
      TIME_ELAPSED=$(( $TIME_NOW - $START_TIME ))

      echo "Start time is $START_TIME"
      echo "Time Now is $TIME_NOW"
      echo "Time elapsed is $TIME_ELAPSED"


     [ "${TIME_ELAPSED}" -gt "${TIMEOUT}" ] && exit
      sleep $RETRY_DELAY

  done
}

function runEventLogLiquibase() {
    echo "Executing event log Liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-repository-liquibase:${EVENT_LOG_VERSION}:jar
    java -jar target/event-repository-liquibase-${EVENT_LOG_VERSION}.jar --url=jdbc:postgresql://localhost:5432/progressioneventstore --username=progression --password=progression --logLevel=info update
    echo "Finished executing event log liquibase"
}

function runEventLogAggregateSnapshotLiquibase() {
    echo "Running EventLogAggregateSnapshotLiquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:aggregate-snapshot-repository-liquibase:${EVENT_LOG_VERSION}:jar
    java -jar target/aggregate-snapshot-repository-liquibase-${EVENT_LOG_VERSION}.jar --url=jdbc:postgresql://localhost:5432/progressioneventstore --username=progression --password=progression --logLevel=info update
    echo "Finished executing EventLogAggregateSnapshotLiquibase liquibase"
}

function runEventBufferLiquibase() {
    echo "running event buffer liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-buffer-liquibase:${EVENT_BUFFER_VERSION}:jar
    java -jar target/event-buffer-liquibase-${EVENT_BUFFER_VERSION}.jar --url=jdbc:postgresql://localhost:5432/progressionviewstore --username=progression --password=progression --logLevel=info update
    echo "finished running event buffer liquibase"
}

function runViewStoreLiquibase {
  #run liquibase for context
  mvn -f ${CONTEXT_NAME}-viewstore/${CONTEXT_NAME}-viewstore-liquibase/pom.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/progressionviewstore -Dliquibase.username=progression -Dliquibase.password=progression -Dliquibase.logLevel=info resources:resources liquibase:update
  echo "Finished executing liquibase"
}

function integrationTests {
  echo
  echo "Running Integration Tests"
  mvn -f ${CONTEXT_NAME}-integration-test/pom.xml clean integration-test -P${CONTEXT_NAME}-integration-test -DtrimStackTrace=false -DINTEGRATION_HOST_KEY=localhost
  echo "Finished running Integration Tests"
}


function deployWiremock() {
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=$WILDFLY_DEPLOYMENT_DIR -Dartifact=uk.gov.justice.services:wiremock-service:1.1.0:war
}

function buildDeployAndTest {
  buildWars
  deployAndTest
}
function deployWiremock() {
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=$WILDFLY_DEPLOYMENT_DIR -Dartifact=uk.gov.justice.services:wiremock-service:1.1.0:war
}

function deployAndTest {
  runEventLogLiquibase
  runEventLogAggregateSnapshotLiquibase
  runViewStoreLiquibase
  runEventBufferLiquibase
}

deployAndTest
