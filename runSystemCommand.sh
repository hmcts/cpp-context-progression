#!/usr/bin/env bash

##################################################################################################
#
# Wrapper script around the framework-jmx-command-client.jar JMX client to make running easier
#
# Usage:
#
# To run a command (e.g. CATCHUP):
#   ./runSystemCommand.sh <command name>
#
# To list all commands:
#   ./runSystemCommand.sh
#
# To run --help against the java client jar
#   ./runSystemCommand.sh --help
#
##################################################################################################

FRAMEWORK_VERSION=8.0.8
CONTEXT_NAME="progression"
USER_NAME="admin"
PASSWORD="admin"
JAR=target/framework-jmx-command-client-${FRAMEWORK_VERSION}.jar

#fail script on error
set -e

echo
echo "Framework System Command Client for '$CONTEXT_NAME' context"

if [ ! -f "$JAR" ]; then
    echo "Downloading artifacts..."
    echo
    mvn --quiet org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:framework-jmx-command-client:${FRAMEWORK_VERSION}:jar
fi

if [ -z "$1" ]; then
  echo "Listing commands"
  echo
  java -jar "$JAR" -l -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
elif [ "$1" == "--help" ]; then
  java -jar "$JAR" --help -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
fi

COMMAND=$1

if [ ! -z "$2" ]; then
  COMMAND_RUNTIME_ID=$2
  echo "Running command '$COMMAND' with command runtime id '$COMMAND_RUNTIME_ID'"
  echo
  echo "Starting JMX client..."
  java -jar "$JAR" -c "$COMMAND" -crid "$COMMAND_RUNTIME_ID" -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
else
  echo "Running command '$COMMAND'"
  echo
  echo "Starting JMX client..."
  java -jar "$JAR" -c "$COMMAND" -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
fi
