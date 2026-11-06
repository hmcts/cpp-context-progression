# Step 04 Partial JPA query pilot

This branch makes one RelatedReference finder explicit using the existing Javax
JPA API. The case predicate and list cardinality remain the same; no ordering,
limit, fallback or business selection rule is introduced.

The repository remains a DeltaSpike repository and extends
AbstractEntityRepository. Its inherited CRUD operations, repository factory,
EntityManager lifecycle and transaction strategy remain in place. Concrete query
methods on abstract repositories already exist in this service. The Step 02
container tests exercise the pilot's public API, filtering and CRUD outcomes.

This small pilot deliberately does not implement a new persist/merge policy or
take ownership of flush, commit, rollback, EntityManager creation or disposal.
Those changes require exact resolved framework/dependency evidence and container
tests. A query-only pilot cannot demonstrate those lifecycle contracts.

## Acceptance boundary

This is preparatory work within Step 04, not completion of that roadmap step or
DeltaSpike removal. The baseline and characterization branches must pass first.
The actual released DeltaSpike/provider versions, CDI resolution, existing CRUD
tests and query parity must be verified in CI before this pilot is accepted.
Changing an interface to an abstract class requires a clean full-reactor rebuild
and repackaging of its consumers; do not mix old compiled callers with this class.
PostgreSQL, WildFly/JTA, detached-instance semantics, shared persistence-context
identity, flush policy, rollback and concurrency remain open acceptance gates.

Keep this branch unmerged and undeployed until the required evidence is reviewed.
Do not start broad repository conversion on the strength of the H2 fixture alone.
