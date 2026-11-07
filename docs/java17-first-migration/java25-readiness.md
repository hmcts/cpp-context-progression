# Java 25 next steps and prerequisite evidence

Step 11 starts the Java 25 application pilot from current Progression `main` at
`8917013f5f235b0c1da7597541d53f042e472173`, in parallel with Step 10. Its branch is
`migration/progression-11-java25-application-pilot`. Refresh the Java 17 comparison
in Steps 01–04. Steps 05–09 remain available for an optional Java 17 release.
Remaining conversions default to reviewable Java 25 batches after pilot proof.

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

This is upstream runtime evidence for one repository slice. Full Progression
generation and packaging, the exact HMCTS image and framework transaction
lifecycle, comparison with the refreshed Java 17 baseline and both executed MI
contracts remain open. The Step 10 managed dependency and MI findings still
apply to the selected HMCTS target dependency chain.

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

The Step 01–04 drafts and Step 10 probe remain tied to the original baseline
`ceebd3c81`. Main gained PR 683's civil-case work at
`2e29ed5f8cf3282e445b9032a9ac100bdb8fc40d`, with business fixtures, contract
fields, a database index and dependency updates. The pilot starts from
`8917013f5f235b0c1da7597541d53f042e472173`, which advances the development version
to `17.104.309-SNAPSHOT`.

`ProsecutionCaseRepository` gained the PostgreSQL-native `countActiveGroupMembers`
method, which filters JSONB `isGroupMember`. Refresh its method and caller
inventory and characterise count/exclusion outcomes before converting it on either
route. Preserve the Listing, core-domain, Results, application court orders and SJP
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

## Work to qualify and expand the pilot

| Work | Next action | Evidence required |
|---|---|---|
| Steps 01–04 comparison baseline | Refresh and retest current main, including the added civil-case repository method and business fixtures | Retain exact source and runtime, dependency manifest, caller contracts, lifecycle and outcome evidence |
| Optional Steps 05–09 on Java 17 | Choose this route only if an intermediate release is needed. Preserve the cases, documents and remaining repository groups and retire DeltaSpike after qualification | Close the Java 17 PostgreSQL/JTA and business gates before releasing the optional checkpoint |
| Step 10 target prerequisites | Resolve Hibernate alignment and pin compatible releases of both MI classifiers. Follow up the cancelled Java 17 verification with the CI owner | Platform and MI publishers supply reviewed compatible versions. Run both MI contract classifiers and retain actual results |
| Step 11 application pilot | Start from current main now. Probe coherent Java 25/Jakarta source, descriptors, generators, test support and necessary ORM adaptations | Keep main-owned clients/models, rules and assertions. Compare with current main and prove representative target lifecycle before expanding |
| Step 12 target runtime | Prepare service-specific configuration after qualified Step 10 and finish against the buildable Step 11 artifact | Verify image digest, JVM/provider, PostgreSQL/JTA and CDI/JMS/JNDI/security in an authorised isolated test environment |
| Step 11 target batches | Convert case/hearing, document and remaining groups on Java 25 by default after pilot proof | Review each method and caller, preserve cardinality, transaction and business outcomes, and retire DeltaSpike only after all usages are accounted for |
| Step 13 target assurance | Stack evidence on Step 11 and run the final combined SHA with matched Step 12 configuration | Reconcile executed and skipped tests, including both MI classifiers. Accept source parity, PostgreSQL/JTA, performance and rollback evidence |
| Step 14 rollout evidence | Prepare release order, immutable manifest, monitoring and stopping thresholds | Release owner authorises deployment separately and verifies actual runtime and business outcomes |

Full branch names and acceptance criteria are in the [branch register](branch-register.csv)
and [work packages](work-packages.json).
