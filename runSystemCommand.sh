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

FRAMEWORK_VERSION=17.100.4
CONTEXT_NAME="progression"
USER_NAME="admin"
PASSWORD="admin"
JAR=target/framework-jmx-command-client-${FRAMEWORK_VERSION}.jar

# fail script on error
set -e

echo
echo "Framework System Command Client for '$CONTEXT_NAME' context"

# Check if framework-jmx-command-client.jar has been downloaded to the ./target directory. If not: download it.
if [ ! -f "$JAR" ]; then
    echo "Downloading artifacts..."
    echo
    mvn --quiet org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:framework-jmx-command-client:${FRAMEWORK_VERSION}:jar
fi

# if no args or --help or -h then just print the help and exit
if [[ $# == 0 || "$1" == "--help" ||  "$1" == "-h" ]]; then
  java -jar "$JAR" --help -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
  exit 0
fi

# If --list or -l then just list the available commands and exit
if [[ "$1" == "--list" || "$1" == "-l" ]]; then
  echo "Listing commands"
  echo
  java -jar "$JAR" -l -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
  exit 0
fi

COMMAND=$1
COMMAND_RUNTIME_ID=""
COMMAND_RUNTIME_STRING=""
COMMAND_RUNTIME_ID_SWITCH=""
COMMAND_RUNTIME_STRING_SWITCH=""

echo "Running command '$COMMAND'"

if [ ! -z "$2" ]; then
   COMMAND_RUNTIME_ID=$2
   printf "command runtime id:\t'$COMMAND_RUNTIME_ID'\n"
fi
if [ ! -z "$3" ]; then
   COMMAND_RUNTIME_STRING=$3
   printf "command runtime string:\t'$COMMAND_RUNTIME_STRING'\n"
fi
if [ ! -z "$COMMAND_RUNTIME_ID" ]; then
  COMMAND_RUNTIME_ID_SWITCH="--command-runtime-id $COMMAND_RUNTIME_ID"
fi
if [ ! -z "COMMAND_RUNTIME_STRING" ]; then
  COMMAND_RUNTIME_STRING_SWITCH="--command-runtime-string $COMMAND_RUNTIME_STRING"
fi
if [ ! -z "$2" ]; then
  echo "Starting JMX client..."
  JAVA_COMMAND="java -jar $JAR -c $COMMAND $COMMAND_RUNTIME_ID_SWITCH -c $COMMAND $COMMAND_RUNTIME_STRING_SWITCH -u $USER_NAME -pw $PASSWORD -cn $CONTEXT_NAME"
  eval $JAVA_COMMAND
else
  echo "Starting JMX client..."
  java -jar "$JAR" -c "$COMMAND" -u "$USER_NAME" -pw "$PASSWORD" -cn "$CONTEXT_NAME"
fi