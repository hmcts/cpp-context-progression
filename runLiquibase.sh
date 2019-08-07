#!/usr/bin/env bash

#The prerequisite for this script is that vagrant is running
#Script that runs, liquibase, deploys wars and runs integration tests


${VAGRANT_DIR:?"Please export VAGRANT_DIR environment variable to point at atcm-vagrant"}
declare WILDFLY_DEPLOYMENT_DIR="${VAGRANT_DIR}/deployments"
declare CONTEXT_NAME=progression
declare EVENT_LOG_VERSION=1.1.8
declare EVENT_BUFFER_VERSION=1.1.8
declare FILE_SERVICE_VERSION=1.17.2


#fail script on error
set -e

source function.sh

runViewStoreLiquibase


