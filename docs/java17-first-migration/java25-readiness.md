# Java 25 next steps and prerequisite evidence

The next implementation is parallel Step 10: [draft PR 690](https://github.com/hmcts/cpp-context-progression/pull/690),
branch `migration/progression-10-target-prerequisites`. It starts from Step 01
at `010ca5bb632f9d47eff7f9c2bdd03252800561b5`. It does not depend on accepting
the partial Step 04 query pilot. Steps 05–09 remain on Java 17; Step 11 requires
both the accepted Java 17 checkpoint and qualified target prerequisites.

## What the new branch implements

- A standalone POM outside the application's Java 17 reactor, inheriting released
  service-parent-pom 25.104.6. The application parent and production code remain
  unchanged.
- An additional PR job on pool MDV-ADO-AGENT-AKS-01 with demand
  `identifier -equals ubuntu-j25`, using the existing approved Maven secure files.
  The normal Java 17 Maven/Sonar job remains enabled.
- Three executed smoke checks: actual Java 25 JVM; Jakarta APIs, explicit Lombok
  processing and framework EntityManager producer loading; intended Hibernate 7
  provider family without overriding dependency management.
- Static inspection of both MI contract-test classifiers: explicit release pin,
  SHA-256, class counts/versions and legacy Java EE references. This does not
  execute those contracts. Unsupported Java/preview classfiles fail inspection.
- Clean checkout/build output and bounded metadata publication. No database,
  container, application image, schema migration or deployment is invoked.

## Executed evidence and open gates

[CI 950134](https://dev.azure.com/hmcts-cpp/cpp-apps/_build/results?buildId=950134)
tested initial head `884612d1f`. The target job ran with Eclipse Adoptium
Java 25.0.2 and Maven 3.9.16. It resolved the selected private parent chain,
compiled the fixture and executed three Java tests: two passed, one failed,
zero errors and zero skipped. The sole failure reports Hibernate 6.6.1.Final
where the target-family assertion requires 7. The successful checks establish
only this fixture's JVM, API, processor and class-loading results.

The separate MI inspection also failed: `RELEASE` is not an explicit release pin,
and **both** resolved listener-tests and processor-tests contain legacy Java EE
references. CI published `java25-readiness-metadata/readiness.json`. This is a
failed readiness gate, not a missing-artifact diagnosis or executed MI contract
result. Exact binary versions/digests are retained in that CI artifact; they were
not independently retrieved during this review.

Follow-up head `bbd16918ffe7153cd987f7788e378176ccc141cb` adds clean-output
protection and rejects newer/preview classfiles. [CI 950145](https://dev.azure.com/hmcts-cpp/cpp-apps/_build/results?buildId=950145)
re-executed the six inspector tests successfully on Java 25.0.2/Maven 3.9.16, then
reproduced the same Java result (three run, one Hibernate failure, zero errors or
skips) and all three MI inspection errors. Metadata publication succeeded. The
separate normal Java 17 RunVerification job is queued, waiting for an agent to
receive and acknowledge the request. The parent ContextVerify stage being marked
running does not establish execution. No final pipeline pass is available.
Full application generation, compilation, packaging and runtime outcomes have not
been exercised on Java 25.

## Main advanced during preparation

The draft stack remains tied to the original baseline `ceebd3c81`. Main now points
to `2e29ed5f8cf3282e445b9032a9ac100bdb8fc40d`, incorporating PR 683's civil-case work
and development version 17.104.308-SNAPSHOT. This adds business fixtures, contract
fields, a database index and dependency updates. These are existing main changes
to preserve when refreshing the migration; they were not introduced by this probe.

In particular, ProsecutionCaseRepository now has PostgreSQL-native
`countActiveGroupMembers`, filtering JSONB `isGroupMember`. Refresh the method and
caller inventory and characterize count/exclusion outcomes before its Step 05
conversion. Main also updates Listing, core-domain, Results, application court
orders and SJP inputs. The old pinned draft CI does not qualify this new baseline.
Refresh against accepted main and rerun the combined evidence before integration;
no main or draft history was rewritten in this task.

## Source-consistent target candidate

| Component | Candidate input | Evidence boundary |
|---|---|---|
| Service / platform-libraries parent | 25.104.6 / 25.104.6 | Inherited by the executed probe |
| Common parent / application BOM | 25.104.5 / 25.104.5 | Selected chain; not every component shares one version |
| Framework common BOM | 25.104.3 | Probe actually resolves Hibernate 6.6.1.Final |
| Framework / libraries | 25.104.6 / 25.104.5 | Full Progression component/generator set remains unqualified |
| Event store / file service / core domain | 25.104.6 / 25.104.3 / 25.104.6 | Released-source candidates; full application resolution/contract checks remain |
| Persistence replacement | persistence-jpa 25.104.6 | EntityManagerProducer loads; CDI/JTA/lifecycle not proved |
| Target test support | test-utils-hibernate 25.104.5 | Released source; managed provider alignment remains open |
| Lombok | 1.18.40 | Explicit processor matches selected BOM and passed the smoke fixture |
| Both MI classifiers | Compatible explicit release required | Current RELEASE probe fails pin and legacy-API gates |
| Runtime / pipeline | WildFly 40 target | Custom immutable image, modules and configuration not qualified |

Primary source inputs: [service parent](https://github.com/hmcts/cpp-platform-maven-service-parent-pom/blob/25.104.6/pom.xml),
[platform libraries](https://github.com/hmcts/cpp-platform-libraries/blob/25.104.6/pom.xml),
[application BOM](https://github.com/hmcts/cpp-platform-maven-common-bom/blob/25.104.5/pom.xml),
[managed Hibernate](https://github.com/hmcts/cp-maven-common-bom/blob/release-25.104.3/pom.xml#L99),
[JPA replacement](https://github.com/hmcts/cp-microservice-framework/tree/release-25.104.6/persistence/persistence-jpa),
and [Hibernate test support](https://github.com/hmcts/cp-framework-libraries/tree/release-25.104.5/framework-utilities/test-utils/test-utils-hibernate).
Upstream [WildFly 40](https://www.wildfly.org/news/2026/05/21/WildFly-40-is-released/)
ships Hibernate 7.3.2; that does not establish the contents of the custom image.

## Work to unblock the next branches

The CI owner must also resolve the queued Java 17 job and retain its actual final
verification result. No agent restart or pool/configuration change was attempted.


| Step / branch suffix | Concrete next work | Human work / exit gate |
|---|---|---|
| 05 `jpa-cases-hearings` | Convert the 19 case/hearing repositories after lifecycle qualification | Progression and persistence reviewers reconcile methods/callers, cardinality and hearing outcomes |
| 06 `jpa-documents` | Convert the seven document/material repositories | Document/access owners verify joins, visibility, placement and PDF/stream outcomes |
| 07 `jpa-registers-remaining` | Convert the remaining 18 repositories and account for other DeltaSpike users | Workflow owners accept query/date/notification parity; all usages reconciled |
| 08 `remove-deltaspike` | Remove dependencies after caller conversion; preserve lifecycle/flush/timezone ownership | Framework owner supplies a compatible Java 17 replacement where needed; dependency/package and lifecycle evidence |
| 09 `j17-release-evidence` | Reconcile final Java 17 artifact, contracts, PostgreSQL/JTA and business results | Test/service/release owners accept the releasable checkpoint and rollback evidence |
| 10 `target-prerequisites` | Resolve Hibernate alignment and pin compatible releases of both MI classifiers; then qualify generators and exact target image/configuration | Platform and MI owners provide reviewed compatible versions; CI must turn green with no gate waiver |
| 11 `target-application` | After 09/10, migrate coherent parent/toolchain, Jakarta imports/descriptors, generators, test support and ORM adaptations | Review only compatibility changes; retain main-owned clients/models, business rules and all contract assertions |
| 12 `wildfly40-runtime` in cpp-aks-deploy | Prepare service-specific matching image/configuration; complete against the Step 11 artifact | Platform owner verifies digest, JVM/provider, CDI/JTA/JMS/JNDI/security in an isolated authorised test environment |
| 13 `target-assurance` | Stack tests/evidence on Step 11; run its combined SHA with Step 12 configuration | Test/contract/service owners reconcile executed populations and end-to-end outcomes, performance and rollback |
| 14 `rollout-evidence` | Prepare release order, immutable manifest, monitoring and stop criteria | Named release owner authorises deployment separately; retain actual runtime and business outcome evidence |

Full branch names, dependencies and acceptance criteria remain in
[the register](branch-register.csv) and [work packages](work-packages.json).
Steps 05–09 and 11–14 have not been created or implemented. Their prerequisites
are still open; empty placeholder branches would not supply reviewable changes.
No migration PR has been merged and no environment has been deployed by this work.
