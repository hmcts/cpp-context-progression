#!/usr/bin/env bash

# Script that runs, liquibase, deploys wars and runs integration tests

CONTEXT_NAME=progression

FRAMEWORK_LIBRARIES_VERSION=$(mvn help:evaluate -Dexpression=framework-libraries.version -q -DforceStdout)
FRAMEWORK_VERSION=$(mvn help:evaluate -Dexpression=framework.version -q -DforceStdout)
EVENT_STORE_VERSION=$(mvn help:evaluate -Dexpression=event-store.version -q -DforceStdout)

DOCKER_CONTAINER_REGISTRY_HOST_NAME=crmdvrepo01

LIQUIBASE_COMMAND=update
#LIQUIBASE_COMMAND=dropAll

#fail script on error
set -e

[ -z "$CPP_DOCKER_DIR" ] && echo "Please export CPP_DOCKER_DIR environment variable pointing to cpp-developers-docker repo (https://github.com/hmcts/cpp-developers-docker) checked out locally" && exit 1
WILDFLY_DEPLOYMENT_DIR="$CPP_DOCKER_DIR/containers/wildfly/deployments"

source $CPP_DOCKER_DIR/docker-utility-functions.sh
source $CPP_DOCKER_DIR/build-scripts/integration-test-scipt-functions.sh

function runLiquibase {
  runEventLogLiquibase
  runEventLogAggregateSnapshotLiquibase
  runEventBufferLiquibase
  runEventProcessorStoreLiquibase
  runViewStoreLiquibase
  runJobStoreLiquibase
  runSystemLiquibase
  runEventTrackingLiquibase
  runFileServiceLiquibase
  echo "All liquibase $LIQUIBASE_COMMAND scripts run"
}

function buildDeployAndTest {
  loginToDockerContainerRegistry
  buildWars
  undeployWarsFromDocker
  buildAndStartContainersWithElasticSearch
  runLiquibase
  deployWiremock
  deployWars
  healthchecks
  integrationTests
}

buildDeployAndTest

