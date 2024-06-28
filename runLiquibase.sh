#!/usr/bin/env bash

#The prerequisite for this script is that vagrant is running
#Script that runs, liquibase, deploys wars and runs integration tests


${VAGRANT_DIR:?"Please export VAGRANT_DIR environment variable to point at atcm-vagrant"}
declare WILDFLY_DEPLOYMENT_DIR="${VAGRANT_DIR}/deployments"
declare CONTEXT_NAME=progression
declare FRAMEWORK_VERSION=8.0.8
declare EVENT_STORE_VERSION=8.3.8
declare FILE_SERVICE_VERSION=8.0.2


#fail script on error
set -e

source runIntegrationTests.sh

runViewStoreLiquibase

mvn -f ${CONTEXT_NAME}-viewstore/${CONTEXT_NAME}-viewstore-liquibase/pom.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore -Dliquibase.username=${CONTEXT_NAME} -Dliquibase.password=${CONTEXT_NAME} -Dliquibase.logLevel=info resources:resources liquibase:update
