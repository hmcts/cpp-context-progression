# Test support still to build

There is no established ready-made PostgreSQL/current-WildFly transaction gate
for this work. This is engineering work within Steps 03 and 04. The proposed
approach is a dedicated CI test job that starts its own database and application
server with synthetic fixtures, then disposes of them. A permanent shared test
environment is not a prerequisite for that design.

The existing PR job already runs fast H2/OpenEJB tests. Those give useful evidence
about queries and repository calls. The additional job must check the actual
PostgreSQL database behaviour and current application-server transaction path.

| Work in order | What it establishes | Human contribution |
|---|---|---|
| Pin the current Java 17 runtime and provider | Exact image/tag/digest, Javax API, Hibernate, framework and datasource versions used by the test | Build/platform owner qualifies the image and approved pull/build route |
| Start isolated PostgreSQL fixtures | Real SQL types, constraints, row identity and committed visibility after clearing caches | Developer builds setup/cleanup; database reviewer verifies isolation and schema |
| Run the same repository outcomes | Zero/one/multiple matches, exclusions, assigned/composite IDs and relevant save/delete contracts | Test/persistence reviewers select actual entities and reconcile against the baseline |
| Exercise transactions in the current WildFly container | Correct EntityManager/context joining, flush, commit, forced rollback and relevant concurrent updates | Developer adds in-container checks; framework reviewer traces the consumed producer/interceptors |
| Wire a dedicated CI job and retain evidence | The final source/image/configuration ran the expected assertions, with failures, retries and skips visible | Build owner wires the job; test lead reconciles reports; reviewers accept the result |

Forced rollback checks must write and flush inside the actual container
transaction, then prove absence of partial writes using a fresh transaction/read.
Do not infer this from a cached entity, an H2 resource-local rollback or a mocked
transaction annotation. Establish current save/remove and context identity
behaviour before replacing lifecycle ownership.

The partial query pilot bypasses DeltaSpike's derived-query processors and
exception path while retaining inherited CRUD and manager lookup. Include
pending-write visibility and actual exception propagation in the comparison.
Released framework 17.104.0 has a separate conditional post-listener flush hook;
preserve and exercise that outer listener path as well as repository calls.

The source image-build rule selects a WildFly 26.1.3/Java 17 tag prefix for the
current major version. Its final suffix and digest still need qualification.
Using a newer WildFly/Hibernate version here would combine the baseline check
with the later platform transition.

The existing tests-only validation template may be reusable after its prerequisites
are supplied, but it expects an already prepared namespace/agent/routes and its
fixtures can clean stores and recreate indexes. It is not an isolated environment
provisioner. No deployment, migration or such cleanup was triggered in this work.

The full Steps 03/04 gates remain open. Refresh their Java 17 evidence on current
main for the target comparison and complete these gates before releasing the
optional Java 17 route. Step 11 can start in parallel. Before expanding repository
conversion on Java 25, prove representative lifecycle and contract outcomes on
PostgreSQL and the exact target WildFly/JTA configuration. Repeat the applicable
checks on the final combined artifact before release. [Roadmap](roadmap.md)
