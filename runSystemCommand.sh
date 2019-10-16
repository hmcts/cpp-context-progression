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

FRAMEWORK_JMX_COMMAND_CLIENT_VERSION=2.2.1
CONTEXT_NAME="material"
USER_NAME="admin"
PASSWORD="admin"

#fail script on error
set -e

echo
echo "Framework System Command Client for '$CONTEXT_NAME' context"
echo "Downloading artifacts..."
echo
mvn --quiet org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice:framework-jmx-command-client:${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}:jar

if [ -z "$1" ]; then
  echo "Listing commands"
  echo
  java -jar target/framework-jmx-command-client-${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}.jar -l -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
elif [ "$1" == "--help" ]; then
  java -jar target/framework-jmx-command-client-${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}.jar --help -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
else
  COMMAND=$1
  echo "Running command '$COMMAND'"
  echo
  java -jar target/framework-jmx-command-client-${FRAMEWORK_JMX_COMMAND_CLIENT_VERSION}.jar -c "$COMMAND" -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
fi