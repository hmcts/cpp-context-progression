# Progression Java 25 persistence pilot

The standalone WAR compiles Progression's `RelatedReference` entity from the
checkout. Maven changes its persistence import prefix in a generated copy.
The pilot repository uses an explicit Jakarta Persistence query and assigned
UUIDs. Its container checks cover zero, one and multiple references, case
filtering, an update under the same ID and a flushed write followed by rollback.

## Run

From the repository root, build with Java 25 and Maven. The project resolves
public build dependencies. Use Docker Desktop and the following pinned images.

```sh
mvn -B -C -V -f tools/migration/java25-application-pilot/pom.xml clean verify
docker --context desktop-linux pull quay.io/wildfly/wildfly@sha256:692fdfb236e3f599e9a2dee1642eb14ab25ca4047446b8ef818fa4c609034f67
docker --context desktop-linux pull postgres@sha256:67f41722b7a8cbdb868a44a4995c846eddfdc2973bccb291ce937dce88ad5675
tools/migration/java25-application-pilot/run-local.sh \
  quay.io/wildfly/wildfly@sha256:692fdfb236e3f599e9a2dee1642eb14ab25ca4047446b8ef818fa4c609034f67 \
  postgres@sha256:67f41722b7a8cbdb868a44a4995c846eddfdc2973bccb291ce937dce88ad5675
```

The runner creates an internal network and two disposable containers. It creates
the synthetic table using the current related-reference column definitions,
validates the mapping, and checks the committed data independently with
PostgreSQL. The WAR runs its assertions at deployment. It has no test endpoint.
The runner reports image identities, artifact hashes and runtime versions.

This covers the standalone repository slice on upstream WildFly 40 and
PostgreSQL with container transactions. The Progression reactor, HMCTS runtime
image and framework-managed listener transaction path require separate
qualification, including both MI contract suites. The production reactor stays
on its existing Java 17 configuration.
