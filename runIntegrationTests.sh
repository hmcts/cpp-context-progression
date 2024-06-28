#!/usr/bin/env bash

#The prerequisite for this script is that vagrant is running
#Script that runs, liquibase, deploys wars and runs integration tests


${VAGRANT_DIR:?"Please export VAGRANT_DIR environment variable to point at atcm-vagrant"}
WILDFLY_DEPLOYMENT_DIR="${VAGRANT_DIR}/deployments"
CONTEXT_NAME=progression
FRAMEWORK_LIBRARIES_VERSION=8.0.7
FRAMEWORK_VERSION=8.0.8
EVENT_STORE_VERSION=8.3.8

#fail script on error
set -e

function buildWars {
  echo
  echo "Building wars."
  mvn clean install -nsu
  echo "\n"
  echo "Finished building wars"
}

function startVagrant {
  echo "Starting Vagrant machine from " $VAGRANT_DIR
  export VAGRANT_CWD=$VAGRANT_DIR
  vagrant up;
}

function deleteWars {
  echo
  echo "Deleting wars from $WILDFLY_DEPLOYMENT_DIR....."
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.war
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.deployed
}

function deploySingleWar {
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.undeployed
  find . -name "${CONTEXT_NAME}-service*.war"  -exec cp {} $WILDFLY_DEPLOYMENT_DIR \;
  echo "Copied wars to $WILDFLY_DEPLOYMENT_DIR"
}

function healthCheck {
  CONTEXT=("$CONTEXT_NAME-command-api" "$CONTEXT_NAME-command-handler" "${CONTEXT_NAME}-query-api" "${CONTEXT_NAME}-query-view" "${CONTEXT_NAME}-event-listener" "${CONTEXT_NAME}-event-processor")
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

function integrationTests {
  echo
  echo "Running Integration Tests"
  # To run against embedded ElasticSearch, do the following:
  # 2) run $VAGRANT_DIR/utils/useStandaloneElasticSearch off"
  # 3) remove the "-with-standalone-elasticsearch" in the integration test profile below
  # 4) run the ./runIntegrationTests.sh
  mvn verify -pl ${CONTEXT_NAME}-integration-test -P${CONTEXT_NAME}-integration-test-with-standalone-elasticsearch -DINTEGRATION_HOST_KEY=localhost -Dfailsafe.rerunFailingTestsCount=4
  echo "Finished running Integration Tests"
}

function runEventLogLiquibase() {
    echo "Running EventLogLiquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-repository-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-repository-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}eventstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished EventLogLiquibase"
}

function runEventLogAggregateSnapshotLiquibase() {
    echo "Running EventLogAggregateSnapshotLiquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:aggregate-snapshot-repository-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/aggregate-snapshot-repository-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}eventstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished executing EventLogAggregateSnapshotLiquibase liquibase"
}

function runViewStoreLiquibase {
  echo "Running ViewStoreLiquibase"
  mvn -f ${CONTEXT_NAME}-viewstore/${CONTEXT_NAME}-viewstore-liquibase/pom.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore -Dliquibase.username=${CONTEXT_NAME} -Dliquibase.password=${CONTEXT_NAME} -Dliquibase.logLevel=info resources:resources liquibase:update
  echo "Finished ViewStoreLiquibase"
}

function runJobStoreLiquibase() {
    echo "Running JobstoreLiquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:jobstore-liquibase:${FRAMEWORK_LIBRARIES_VERSION}:jar
    java -jar target/jobstore-liquibase-${FRAMEWORK_LIBRARIES_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}jobstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished executing JobstoreLiquibase liquibase"
}

function runEventBufferLiquibase() {
echo "running EventBufferLiquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-buffer-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-buffer-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
echo "Finished EventBufferLiquibase"
}

function runSystemLiquibase {
    echo "Running system liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:framework-system-liquibase:${FRAMEWORK_VERSION}:jar
    java -jar target/framework-system-liquibase-${FRAMEWORK_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}system --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished executing system liquibase"
}

function runEventTrackingLiquibase {
    echo "Running event tracking liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-tracking-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-tracking-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished executing event tracking liquibase"
}

function runFileServiceLiquibase() {
    echo "running file service liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:file-service-liquibase:${FRAMEWORK_LIBRARIES_VERSION}:jar
    java -jar target/file-service-liquibase-${FRAMEWORK_LIBRARIES_VERSION}.jar --url=jdbc:postgresql://localhost:5432/fileservice --username=fileservice --password=fileservice --logLevel=info update
    echo "finished file service  liquibase"
}


function runLiquibase {
   runEventLogLiquibase
   runEventLogAggregateSnapshotLiquibase
   runEventBufferLiquibase
   runViewStoreLiquibase
   runJobStoreLiquibase
   runSystemLiquibase
   runEventTrackingLiquibase
   runFileServiceLiquibase
   echo "All liquibase update scripts run"
}

function deployWiremock() {
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=$WILDFLY_DEPLOYMENT_DIR -Dartifact=uk.gov.justice.services:wiremock-service:1.1.0:war
}

function buildDeployAndTest {
  buildWars
  deployAndTest
}

function deployAndTest {
  deleteWars
  startVagrant
  runLiquibase
  deployWiremock
  deploySingleWar
  healthCheck
  integrationTests
}

buildDeployAndTest

