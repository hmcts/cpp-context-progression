# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service does

`cpp-context-progression` is the **Progression bounded context** within the HMCTS Crime Common Platform (CPP). It manages case progression through the criminal justice system using the CQRS/Event Sourcing pattern on the CPP microservice framework, deployed as a WAR on WildFly.

## Build commands

```bash
# Full build with unit tests
mvn clean verify

# Build specific module only
mvn -pl progression-service package

# Run unit tests only
mvn test

# Run a single test class
mvn test -pl <module-name> -Dtest=MyTestClass

# Run integration tests (requires Docker — see Local development below)
mvn -P progression-integration-test verify

# Run performance tests
mvn -P progression-performance-test verify
```

## Local development (integration tests)

Integration tests require `cpp-developers-docker` checked out locally.

```bash
export CPP_DOCKER_DIR=/path/to/cpp-developers-docker
./runIntegrationTests.sh
```

This script orchestrates: Docker login → WAR build → container start (WildFly, PostgreSQL, Artemis, Elasticsearch) → Liquibase migrations → WireMock → WAR deployment → health checks → integration tests.

## Module structure

This is a multi-module Maven project (`progression-parent/pom.xml`). The bounded context follows the standard CPP CQRS module layout:

| Module | Purpose |
|--------|---------|
| `progression-domain/` | Aggregate, messages, value objects (4 sub-modules) |
| `progression-command/` | Command API (RAML) + command handlers |
| `progression-query/` | Query API (RAML) + view projection logic |
| `progression-event/` | Event processor, JMS listener, Elasticsearch indexer |
| `progression-event-sources/` | YAML event subscription configuration |
| `progression-viewstore/` | Read model: Liquibase migrations + JPA entities |
| `progression-eventprocessorstore/` | Event processor state: Liquibase migrations + JPA entities |
| `progression-service/` | WAR artifact — bundles all modules for deployment |
| `progression-integration-test/` | Full integration test suite (failsafe, alphabetical order) |
| `progression-performance-test/` | JMeter load tests with Lightning validation |
| `progression-test-utilities/` | POJO generation helpers for tests |
| `progression-refdata-service/` | Reference data / material data lookups |
| `progression-healthchecks/` | Health check endpoints |

## CQRS architecture

- **Commands** flow through REST (RAML-defined) → command handlers → event store
- **Queries** read from the view store (PostgreSQL, JPA) via RAML-defined REST APIs
- **Events** are consumed by the event processor, which updates the view store
- **Messaging**: JMS (embedded Artemis) for intra-context; Azure Service Bus for cross-context
- **RAML contracts** drive pojo generation via `raml-maven-plugin` and messaging adapter generation

## Package conventions

```
uk.gov.moj.cpp.progression.command.*    # Command APIs
uk.gov.moj.cpp.progression.query.*      # Query APIs
uk.gov.moj.cpp.progression.domain.*     # Domain model (excluded from SonarQube)
uk.gov.moj.cpp.progression.event.*      # Event processing
uk.gov.moj.cpp.progression.persistence.*# DB entities (excluded from SonarQube)
uk.gov.moj.cpp.progression.it.*         # Integration test POJOs
```

## CI/CD

- **PR pipeline**: `azure-pipelines.yaml` → `context-verify.yaml` (SonarQube)
- **Merge pipeline**: `context-validation.yaml` (full build + integration tests)
- **Agent pool**: `MDV-ADO-AGENT-AKS-01`
- **SonarQube project**: `uk.gov.moj.cpp.progression:progression-parent`
- **Release management**: `jgitflow-maven-plugin`; release branches follow `dev/release-*` pattern

## Coding standards

Full standards are in `.claude/context/coding-standards.md`. Key rules:

- Methods: ≤20 lines target, 40 lines hard limit; one responsibility per method
- Test classes: suffix `Test` for unit, `IT` for integration
- Commit messages: Conventional Commits format — `feat(scope): summary` with Jira ticket in footer
- PR titles must include the Jira ticket: `[PROJ-123] Description`
- Max 400 lines changed per PR

## Tech stack reference

`.claude/context/tech-stack.md` is the authoritative reference for CPP technology choices, version matrix, and guidance on when to use CQRS vs Modern by Default (MbD) patterns. Consult it before making tooling or architectural decisions.
