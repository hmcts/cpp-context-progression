# Java 25 next steps and prerequisite evidence

The full Java 25 reactor starts from live Progression `main` at
`f3dbf8c897e74302e5b3c54d22e4cd315c6b21de` on
`migration/progression-11-java25-full-build`. Refresh the Java 17 comparison on the same main revision and qualify the
actual HMCTS platform and MI inputs in parallel. The wider repository conversion
follows proof of an actual Progression listener using the matched target
PostgreSQL and WildFly/JTA path. [Roadmap](roadmap.md)

## Step 11 first application slice

[Draft PR 691](https://github.com/hmcts/cpp-context-progression/pull/691) packages
the RelatedReference entity from the checkout with Jakarta persistence imports
and a candidate repository in a standalone test WAR. Maven 3.9.11 on JDK 25.0.1
built commit `73bf319c2abc700ac07686317feed5224de88e83`. The WAR ran in disposable
upstream WildFly 40.0.1.Final on Java 25.0.3 with server Hibernate 7.3.2.Final and
PostgreSQL 17.11. Zero, one and multiple matches, case filtering, an assigned-ID
update and a flushed rollback passed. A separate database connection verified
three committed rows across two cases, one updated row and zero rolled-back rows.
The runner removed both containers and their network.

This historical slice establishes one repository outcome set on upstream
WildFly and PostgreSQL. Use it to design checks for the full build from live main.
The full Progression reactor, the exact HMCTS image and actual application
listener lifecycle, refreshed Java 17 comparison and executed MI contracts all
need evidence on the new route. Step 10 records the managed dependency and MI
findings for its selected input chain.

## Step 10 prerequisite probe

Step 10 is [draft PR 690](https://github.com/hmcts/cpp-context-progression/pull/690)
on `migration/progression-10-target-prerequisites`, based on Step 01 at
`010ca5bb632f9d47eff7f9c2bdd03252800561b5`. The following evidence describes
that isolated prerequisite probe.

## Step 10 probe scope

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
separate normal Java 17 RunVerification job was cancelled after agent loss.
Treat the Java 17 job as incomplete and retain the target job's failed result.
This run exercised only the probe fixture. Full application generation,
compilation, packaging and runtime outcomes still require separate evidence.

## Main advanced during preparation

The Step 01–04 drafts and Step 10 probe used the original baseline `ceebd3c81`.
Main gained PR 683's civil-case work at `2e29ed5f8cf3282e445b9032a9ac100bdb8fc40d`,
including business fixtures, contract fields, a database index and updated
dependencies. The upstream slice in PR 691 used `8917013f5f235b0c1da7597541d53f042e472173`.
Refresh the Java 17 comparison for the new full-build base `f3dbf8c897e74302e5b3c54d22e4cd315c6b21de`.

`ProsecutionCaseRepository` gained the PostgreSQL-native `countActiveGroupMembers`
method, which filters JSONB `isGroupMember`. Refresh its method and caller
inventory and characterise count/exclusion outcomes before converting it in the Java 25 batch. Preserve the Listing, core-domain, Results, application court orders and SJP
inputs unless a reviewed target dependency change is required. Repeat the Java 17
baseline checks on current main and reconcile the exact final target candidate
against accepted main before integration.

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

The [pinned HMCTS image recipe](https://github.com/hmcts/cpp-docker-images/blob/0c034a6e9e3c92291573d378e4922200cb711ca5/wildfly40-ubuntu.dockerfile#L18)
selects WildFly 40.0.1.Final; its [metadata](https://github.com/hmcts/cpp-docker-images/blob/0c034a6e9e3c92291573d378e4922200cb711ca5/metadata-wildfly.json#L6)
supplies a Java 25 base input. The [official server POM](https://github.com/wildfly/wildfly/blob/40.0.1.Final/pom.xml#L545)
declares Hibernate 7.3.2.Final. These strengthen the source candidate only. The
pipeline owner must supply the exact consumed cpp.pipeline revision and
`aks-pipeline.versions.yml` value of `widlfly40_base_tag`; existing noninteractive
access could not authenticate to refresh that source. The complete image tag,
digest and actual running provider remain unqualified.

Primary source inputs: [service parent](https://github.com/hmcts/cpp-platform-maven-service-parent-pom/blob/25.104.6/pom.xml),
[platform libraries](https://github.com/hmcts/cpp-platform-libraries/blob/25.104.6/pom.xml),
[application BOM](https://github.com/hmcts/cpp-platform-maven-common-bom/blob/25.104.5/pom.xml),
[managed Hibernate](https://github.com/hmcts/cp-maven-common-bom/blob/release-25.104.3/pom.xml#L99),
[JPA replacement](https://github.com/hmcts/cp-microservice-framework/tree/release-25.104.6/persistence/persistence-jpa),
and [Hibernate test support](https://github.com/hmcts/cp-framework-libraries/tree/release-25.104.5/framework-utilities/test-utils/test-utils-hibernate).
Upstream [WildFly 40](https://www.wildfly.org/news/2026/05/21/WildFly-40-is-released/)
ships Hibernate 7.3.2; that does not establish the contents of the custom image.

## Work to qualify the full build and cutover

| Work | Next action | Evidence required |
|---|---|---|
| Step 11 full reactor | Build and package from live main on Java 25 and Jakarta. Record resolved dependencies and any build blockers | Full module, generator and test-artifact output with exact source, toolchain and executed assertions |
| Steps 01–04 comparison | Refresh the Java 17 run on the new live-main revision | Baseline dependency, runtime, caller contract and business outcome evidence on that revision |
| Step 10 HMCTS inputs | Pin consumed platform artifacts, agent/template/chart/image and both compatible MI classifiers. Follow up the cancelled Java 17 verification with its CI owner | Qualified provider family and source-to-runtime manifest. Compatible pinned MI inputs for actual contract execution |
| Step 12 actual application listener | Use the Step 11 artifact on the exact target runtime and PostgreSQL. Exercise the framework JTA path before wider conversion | EntityManager joining, pending-write visibility, post-listener flush, commit and forced rollback proved through the actual listener. Verify committed and rolled-back rows independently |
| Steps 05–07 parallel conversions | Convert cases/hearings, documents and remaining repositories in sibling Java 25 batches from the qualified base | Per-method and caller results preserve cardinality, filtering, paging, transaction and business outcomes |
| Step 08 combined conversion | Combine the reviewed batches and retire DeltaSpike after accounting for every caller | Combined application and packaged dependency evidence with lifecycle and outcome parity |
| Step 13 combined assurance | Run the final source, image and runtime configuration through one green pipeline | Executed PostgreSQL/JTA, both MI classifiers and critical journeys, current-main parity, performance and rollback evidence |
| Step 14 cutover | Authorise one coordinated source and runtime change | Release manifest, monitoring, stopping criteria and verified running business outcomes |
| Step 09 optional release | Revisit a separate Java 17 release only for identified business value | Separate scope, approval and its own release evidence |

Full branch names and acceptance criteria are in the [branch register](branch-register.csv)
and [work packages](work-packages.json).
