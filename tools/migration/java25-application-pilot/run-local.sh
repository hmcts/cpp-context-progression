#!/usr/bin/env bash
set +x
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

wildfly_ref=${1:-quay.io/wildfly/wildfly:40.0.1.Final-jdk25}
postgres_ref=${2:-postgres:17}
if [[ $# -gt 2 || ! -s target/progression-java25-pilot.war || ! -s target/postgresql.jar ]]; then
    printf 'Build both target artifacts first, then run %s [wildfly-image] [postgres-image].\n' "$0" >&2
    exit 1
fi

docker_local() { docker --context desktop-linux "$@"; }
docker_endpoint=$(docker context inspect desktop-linux --format '{{.Endpoints.docker.Host}}')
[[ $docker_endpoint == unix://* ]] || { printf 'A local Docker Desktop socket is required.\n' >&2; exit 1; }
wildfly_meta=$(docker_local image inspect --format '{{.Id}} {{join .RepoDigests ","}}' -- "$wildfly_ref")
postgres_meta=$(docker_local image inspect --format '{{.Id}} {{join .RepoDigests ","}}' -- "$postgres_ref")
printf 'WildFly image %s %s\nPostgreSQL image %s %s\n' "$wildfly_ref" "$wildfly_meta" "$postgres_ref" "$postgres_meta"
shasum -a 256 target/progression-java25-pilot.war target/postgresql.jar

suffix=$(openssl rand -hex 8)
POSTGRES_PASSWORD=$(openssl rand -hex 24)
export POSTGRES_PASSWORD
network_id= postgres_id= wildfly_id=
redact() {
    local line
    while IFS= read -r line || [[ -n $line ]]; do
        printf '%s\n' "${line//"$POSTGRES_PASSWORD"/[redacted]}"
    done
}
cleanup() {
    local status=$? container_id
    trap - EXIT HUP INT TERM
    if ((status != 0)); then
        for container_id in "$wildfly_id" "$postgres_id"; do
            [[ -z $container_id ]] || docker_local logs --tail 60 "$container_id" 2>&1 | redact >&2 || true
        done
    fi
    for container_id in "$wildfly_id" "$postgres_id"; do
        [[ -z $container_id ]] || docker_local rm -fv "$container_id" >/dev/null || status=1
    done
    [[ -z $network_id ]] || docker_local network rm "$network_id" >/dev/null || status=1
    exit "$status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

wait_for() {
    local label=$1 deadline=$((SECONDS + 90))
    shift
    until "$@" >/dev/null 2>&1; do
        if ((SECONDS >= deadline)); then
            printf 'Timed out waiting for %s.\n' "$label" >&2
            return 1
        fi
        sleep 1
    done
}
cli() {
    docker_local exec "$wildfly_id" /opt/jboss/wildfly/bin/jboss-cli.sh --connect \
        --timeout=2000 --command-timeout=60 --command="$1" 2>&1 | redact
}
wildfly_ready() { cli ':read-attribute(name=server-state)' | grep -F '"result" => "running"'; }
pilot_marker() { docker_local logs "$wildfly_id" 2>&1 | grep -F 'PROGRESSION_JAVA25_PILOT_PASS'; }

network_id=$(docker_local network create --internal "progression-pilot-$suffix")
postgres_id=$(docker_local run --pull=never -d --name "progression-pilot-db-$suffix" \
    --network "$network_id" --network-alias postgres --env POSTGRES_PASSWORD \
    --env POSTGRES_DB=progression --env POSTGRES_USER=progression "${postgres_meta%% *}")
wait_for 'PostgreSQL' docker_local exec "$postgres_id" pg_isready -h 127.0.0.1 -U progression -d progression -t 2
# Synthetic fixture matching the current RelatedReference table columns and constraints.
docker_local exec -i "$postgres_id" psql -X -v ON_ERROR_STOP=1 -U progression -d progression <<'SQL'
CREATE TABLE related_reference (
    id uuid PRIMARY KEY,
    prosecution_case_id uuid NOT NULL,
    reference varchar(500) NOT NULL
);
SHOW server_version;
SQL

wildfly_id=$(docker_local run --pull=never -d --name "progression-pilot-app-$suffix" \
    --network "$network_id" --env POSTGRES_PASSWORD "${wildfly_meta%% *}")
wait_for 'WildFly management' wildfly_ready
cli ':read-attribute(name=product-version)'
docker_local cp target/postgresql.jar "$wildfly_id:/tmp/postgresql.jar"
docker_local cp target/progression-java25-pilot.war "$wildfly_id:/tmp/progression-java25-pilot.war"
cli 'deploy /tmp/postgresql.jar'
cli '/subsystem=datasources/data-source=ProgressionPilot:add(jndi-name="java:jboss/datasources/ProgressionPilot",driver-name="postgresql.jar",connection-url="jdbc:postgresql://postgres:5432/progression",user-name=progression,password="${env.POSTGRES_PASSWORD}",jta=true,enabled=true)'
cli '/subsystem=datasources/data-source=ProgressionPilot:test-connection-in-pool'
cli 'deploy /tmp/progression-java25-pilot.war'
wait_for 'deployment test marker' pilot_marker
pilot_marker | redact
database_result=$(docker_local exec "$postgres_id" psql -XAt -v ON_ERROR_STOP=1 -U progression -d progression -c "
    SELECT count(*), count(DISTINCT prosecution_case_id),
           count(*) FILTER (WHERE reference = 'updated'),
           count(*) FILTER (WHERE reference = 'rolled back')
    FROM related_reference")
[[ $database_result == '3|2|1|0' ]] || { printf 'Unexpected database counts: %s\n' "$database_result" >&2; exit 1; }
printf 'PostgreSQL confirmed 3 committed rows across 2 cases, 1 same-ID update and 0 rolled-back rows.\n'
