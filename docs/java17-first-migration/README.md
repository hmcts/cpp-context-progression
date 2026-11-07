# Progression Java 25 pilot and migration plan

Start Step 11 from current Progression `main` at `8917013f5f235b0c1da7597541d53f042e472173` while Step 10 qualifies the target platform. Keep Steps 01–04 as the Java 17 baseline and refresh their evidence against current main. Steps 05–09 are an optional Java 17 release lane. The default route converts remaining repositories in reviewable batches on Java 25 once the application pilot and its target lifecycle are proved. [Implementation status](implementation-status.md)

The original draft stack used `ceebd3c81e899e1dfea4814de9016a49512d806a`. Main gained PR 683's civil-case work at `2e29ed5f8cf3282e445b9032a9ac100bdb8fc40d` and advanced to `17.104.309-SNAPSHOT`. Refresh the method inventory and comparison fixtures before repository conversion. [Main baseline changes](java25-readiness.md#main-advanced-during-preparation)

## Outcome and scope

Preserve the current main contracts, Material and Listing adapters/models, and business outcomes. Route organisation fallback, contact corrections and date-policy changes through separate defect or business change review. Use the [original 44-repository inventory](repository-conversion-inventory.csv) to scope the cases and hearings, documents and remaining batches. It assigns 19, seven and 18 repositories respectively. Refresh methods and callers against current main before confirming batch scope. Preserve used CRUD, cardinality, paging, flush, timezone and transaction behaviour as each group moves to explicit JPA.

## Platform boundary

The selected released service-parent 25.104.6 chain enforces Java 25. Step 10 ran its isolated Java 25 probe and exposed a Hibernate provider mismatch and incompatible/unpinned MI test inputs. The Step 11 draft can expose application compatibility gaps while the artifact publishers resolve those prerequisites. Qualify effective dependencies, generated/packaged output and the exact WildFly 40 image with PostgreSQL/JTA and executed contracts before expanding conversion. [Target evidence and open gates](java25-readiness.md)

## Branch and release rules

- Record each branch base and accepted evidence. Refresh Steps 01–04 against current main for the comparison and reconcile the final target candidate against the current accepted source.
- Start Step 11 directly from current main while Step 10 progresses. Use the optional 05–09 route only if the team elects an intermediate Java 17 release. Step 09 is optional.
- Use the Step 11 pilot and matched Step 12 runtime to prove representative persistence lifecycle and contracts before broad Java 25 conversion. Stack or group the case/hearing, document and remaining conversions as independently reviewable target changes. Use a single disposition per repository across the two routes.
- Step 12 uses its own deployment repository PR. Step 13 tests can target the unmerged Step 11 branch with a buildable artifact and matched configuration. Integrate the tests and rerun the exact final SHA, image digest and configuration before acceptance.
- Step 14 records release authority, monitoring, rollback and observed outcomes on accepted target main. Keep operational payloads and logs outside Git.

## Visual roadmap

[Open the visual roadmap](roadmap.md)

## Branch register

Dependencies in the table describe predecessor evidence or buildable outputs. Step 11 can start immediately. Step 10 and representative target qualification still govern expansion and acceptance.

| Step | Repository | Branch | Start dependency |
|---|---|---|---|
| 00 | cpp-context-progression | `plan/progression-java17-first-migration` | none |
| 01 | cpp-context-progression | `migration/progression-01-j17-baseline` | 00 |
| 02 | cpp-context-progression | `migration/progression-02-contract-characterisation` | 01 |
| 03 | cpp-context-progression | `migration/progression-03-j17-persistence-harness` | 02 |
| 04 | cpp-context-progression | `migration/progression-04-jpa-lifecycle` | 03 |
| 05 optional | cpp-context-progression | `migration/progression-05-jpa-cases-hearings` | 04 |
| 06 optional | cpp-context-progression | `migration/progression-06-jpa-documents` | 05 |
| 07 optional | cpp-context-progression | `migration/progression-07-jpa-registers-remaining` | 06 |
| 08 optional | cpp-context-progression | `migration/progression-08-remove-deltaspike` | 07 |
| 09 optional | cpp-context-progression | `migration/progression-09-j17-release-evidence` | 08 |
| 10 | cpp-context-progression | `migration/progression-10-target-prerequisites` | 01 |
| 11 | cpp-context-progression | `migration/progression-11-java25-application-pilot` | none |
| 12 | cpp-aks-deploy | `migration/progression-12-wildfly40-runtime` | 10 |
| 13 | cpp-context-progression | `migration/progression-13-target-assurance` | 11, 12 |
| 14 | cpp-context-progression | `migration/progression-14-rollout-evidence` | 13 |

## Human work

| Role to assign | Human work and decision | Steps |
|---|---|---|
| Progression technical lead | Agree scope and assign owners; review method/caller inventory and each PR; preserve current main fixes. | 00 to 14 |
| Build and platform owners | Supply authorised artifact access, exact released dependency/source manifest, Java 25 agents, matched image/configuration and compatible contract-test harnesses. Qualify any optional bridge explicitly. | 01 03 04 08 10 to 12 |
| Persistence and framework reviewer | Establish transaction/EntityManager ownership from actual artifacts; review lifecycle, cardinality, paging and SQL; approve retirement of framework support only after equivalence evidence. | 02 to 08 11 13 |
| Test lead and database support | Define representative PostgreSQL and container fixtures, execute meaningful assertions, reconcile discovery/execution/skips, compare baseline performance and retain results. | 01 to 09 11 to 13 |
| Service and business owner | Confirm preserved business outcomes and critical journeys. Decide genuine ambiguities; route business-rule changes through separate approval rather than bundling them into migration. | 00 02 09 11 13 14 |
| MI Listing and Material contract owners | Review schemas, request/response fixtures and model compatibility. Supply compatible consumer checks and verify the downstream outcomes in scope. | 02 10 11 13 |
| Release and operations owners | Approve environment scope and releases, rehearse rollback, agree monitoring and stop thresholds, execute authorised rollout and verify operational/business outcomes. | 09 12 to 14 |

Automation can assist with inventories, code and test drafts, report collection and rendering. Humans still own acceptance rules, independent reviews, artifact/environment access, interpretation of anomalies and release authority. These are proposed roles; named assignees, capacity and estimates have not been agreed.

## Step by step work packages

### 00 Agree the migration plan

`plan/progression-java17-first-migration` in `cpp-context-progression`. Base is origin/main at ceebd3c81e899e1dfea4814de9016a49512d806a.

Own the scope, branch register, undated roadmap, acceptance gates and evidence model. Use current main for the application pilot and preserve unrelated main work. Keep business fixes and model redesign in separate changes.

**Human work**

Technical lead and service owner assign accountable people and agree the phased release policy. Platform, test and operations leads confirm dependencies and evidence expectations.

**Acceptance evidence**

Each step has one scope, branch, proposed owner role and objective exit criteria; implementation and release remain pending.

**Integration and release**

Planning documents only. Implementation progress is recorded separately; no merge or deployment is implied.

### 01 Reproduce the Java 17 baseline

`migration/progression-01-j17-baseline` in `cpp-context-progression`. Original draft base is origin/main at ceebd3c81e899e1dfea4814de9016a49512d806a.

Record JDK/Maven, resolved parent/BOM, effective POM, dependencies, runtime image/configuration, discovered/executed/skipped tests and existing failures. Preserve all current main fixes.

Human work: Build owner supplies authorised private artifact access and a repeatable environment. Test lead distinguishes baseline defects, environmental failures and migration regressions; check owners triage failed gates.

Done when: Clean pinned baseline build and relevant journeys run in the owning environment, with retained logs and explicit failure disposition. No tests silently disappear.

Merge and release rule: Remain on Java 17 and the existing API/runtime stack. Evidence or safe build fixes only.

### 02 Characterise contracts and business outcomes

`migration/progression-02-contract-characterisation` in `cpp-context-progression`. Original draft base is stacked on Step 01 at 010ca5bb632f9d47eff7f9c2bdd03252800561b5; predecessor fixes merged forward.

Pin zero/one/multiple repository results, ordering/paging, entity lifecycle and emitted events. Assert Material/PDF, Listing and MI contracts; main already owns the two local clients/models.

Human work: Service and contract owners confirm representative fixtures, expected event/projection outcomes and critical journeys. Business owner identifies any intentional difference for a separate change request.

Done when: Baseline assertions execute on Java 17, including ambiguous matches, missing/null values and meaningful exclusions. Required MI tests are discovered and executed with identified artifact versions.

Merge and release rule: No business-rule correction, client redesign or new fallback policy in this branch.

### 03 Prepare independent persistence test support

`migration/progression-03-j17-persistence-harness` in `cpp-context-progression`. Original draft base is stacked on Step 02 at 60f7e8ea7a6102c7cde1fe5110e5907f41a9bfa4; predecessor fixes merged forward.

Introduce or select a Java 17-compatible javax JPA test harness that does not require DeltaSpike test-control. Add isolated PostgreSQL and container transaction coverage while keeping useful fast tests.

Human work: Test/platform owners provide approved fixtures and container access. Persistence reviewer checks that new tests exercise provider/container behaviour rather than mocks alone.

Done when: The same baseline outcomes pass under the selected harness; PostgreSQL-specific queries execute on PostgreSQL and transaction tests are labelled separately from H2/resource-local checks.

Merge and release rule: Keep production runtime, Hibernate, schemas and repository behaviour unchanged. Harness versions must remain compatible with the current stack.

### 04 Prove explicit JPA lifecycle contracts

`migration/progression-04-jpa-lifecycle` in `cpp-context-progression`. Original draft base is stacked on Step 03 at 0ba20fd468ff1bd8ce7ccfe0b160888e72665020; predecessor fixes merged forward.

Implement the smallest javax JPA repository seam/pilot on the existing runtime. Define persist/merge identity, assigned/composite keys, flush/delete and optional single-result behaviour before broad conversion.

Human work: Persistence and framework reviewers trace the exact released EntityManager producer, flush interceptor and transaction ownership. Confirm existing timezone behaviour and its ownership; preserve required policy deliberately.

Done when: Pilot preserves managed-instance, detached-update/removal, zero/one/multiple-result, rollback and concurrency contracts. Required framework lifecycle/flush behaviour is retained. The selected injection approach demonstrably shares/joins the intended persistence context, with explicit ownership of flush, commit and rollback.

Merge and release rule: Use APIs available on the existing JPA version; no Jakarta Persistence 3.2-only helpers. Retain the framework persistence artifact while needed.

### 05 Convert case hearing and defendant repositories

`migration/progression-05-jpa-cases-hearings` in `cpp-context-progression`. Base is latest main with accepted predecessor changes if the optional lane is chosen.

Convert the case/hearing/defendant repository group to explicit javax JPA. Keep queries, signatures, exception/cardinality rules, ordering and paging equivalent. Reuse this inventory and its parity assertions for the default Java 25 batches.

**Human work**

Progression developer implements the group; independent persistence reviewer checks method and caller parity. Test lead runs hearing/defendant regression fixtures.

**Acceptance evidence**

Every in-scope repository/method has a disposition and tests for its actual key grain, excluded rows, optional multiplicity, ordering and paging. Relevant event/projection journeys pass.

**Integration and release**

If the Java 17 lane is chosen, merge and release this group independently once accepted. Keep remaining DeltaSpike repositories working in mixed mode.

### 06 Convert document and material repositories

`migration/progression-06-jpa-documents` in `cpp-context-progression`. Base is latest main with accepted predecessor changes if the optional lane is chosen.

Convert document/material/index and related criteria repositories. Preserve one-to-many joins, fetch results, visibility predicates, case/application placement and material associations.

**Human work**

Progression and document/access reviewers verify the association grain and expected query outcomes. Test lead exercises document access, PDF/stream paths and removal flows.

**Acceptance evidence**

Multi-index fixtures, meaningful exclusions, JSON criteria where applicable, document ordering and returned associations match the baseline. Access and document journeys pass.

**Integration and release**

If the Java 17 lane is chosen, release after this group passes its gate. Preserve Material client and access behaviour.

### 07 Convert remaining repository groups

`migration/progression-07-jpa-registers-remaining` in `cpp-context-progression`. Base is latest main with accepted predecessor changes if the optional lane is chosen.

Convert registers, notifications, plea/COTR and remaining DeltaSpike users identified by the inventory. Review existing direct/JDBC repositories touched by any shared change.

**Human work**

Persistence reviewer signs the complete method inventory. Register/notification journey owners confirm selection rules, dates and expected emitted/visible outcomes.

**Acceptance evidence**

All production DeltaSpike proxy usages are eliminated or explicitly accounted for; latest-per-hearing, native SQL, filtering and notification paths preserve baseline results.

**Integration and release**

If the Java 17 lane is chosen, release after targeted and combined checks on the current Hibernate dialect. Apply required target parser adaptations in the Java 25 lane.

### 08 Retire DeltaSpike and preserve framework behaviour

`migration/progression-08-remove-deltaspike` in `cpp-context-progression`. Base is latest main with accepted predecessor changes if the optional lane is chosen.

Remove Apache DeltaSpike and obsolete test-control dependencies only after all callers are converted. Replace/retain EntityManager lifecycle and active-transaction flush behaviour deliberately; trace transitive and packaged dependencies.

**Human work**

Framework owner validates the exact released artifact responsibilities. Progression reviewer approves replacement ownership; test lead proves commit/rollback and repeated-delivery effects. If a separate Java 17 framework artifact is required, add its owning repository change as a numbered prerequisite with its own branch before closing this step.

**Acceptance evidence**

Effective dependency tree and packaged artifact contain no required-but-removed component and no unintended DeltaSpike runtime/test-control dependency. Lifecycle, flush and timezone outcomes remain agreed and tested.

**Integration and release**

If the Java 17 lane is chosen, keep the framework wrapper where it supplies required behaviour. Close this step once the compatible lifecycle, flush and timezone replacement is proved.

### 09 Accept an optional Java 17 release checkpoint

`migration/progression-09-j17-release-evidence` in `cpp-context-progression`. Base is latest main with accepted predecessor changes if the optional lane is chosen.

If the team selects a Java 17 release, record the cumulative artifact, PostgreSQL/JTA, contract and journey results, performance comparison and rollback rehearsal. Retain runbook and evidence references outside operational data.

**Human work**

Service owner accepts outcome parity; test lead validates reports; operations/release owner authorises any deployment separately and confirms monitoring and stopping thresholds.

**Acceptance evidence**

For the optional lane, prove an independently releasable Java 17 version after Step 08 and reconcile business events, projections and downstream outcomes.

**Integration and release**

Optional intermediate release. Steps 10 and 11 can proceed independently of this checkpoint.

### 10 Prepare the supported target platform

`migration/progression-10-target-prerequisites` in `cpp-context-progression`. Base is parallel on Step 01 at 010ca5bb632f9d47eff7f9c2bdd03252800561b5; refresh before integration.

Record compatible released parent/BOM, framework/core models, generators, MI/other test artifacts, Java 25 agents and WildFly 40 image/configuration. Link owner PRs in platform/deployment repositories where changes are needed.

**Human work**

Platform owners supply tested versions and a compatibility manifest; operations provides a target test environment. Decide whether any temporary Java 17 Jakarta bridge is worth separate support; it is not assumed.

**Acceptance evidence**

Identified artifacts and configuration can support the target service build/test cycle; image/template/chart revisions and required JNDI/datasource/messaging settings are recorded. No skipped contract checks are accepted by default.

**Integration and release**

Progress in parallel with the Step 11 application pilot and the Java 17 baseline refresh. Accept target inputs before expanding conversion on Java 25.

### 11 Start the Java 25 application pilot

`migration/progression-11-java25-application-pilot` in `cpp-context-progression`. Base is current Progression main at 8917013f5f235b0c1da7597541d53f042e472173.

Start with a narrow application compatibility pilot using the selected Java 25 parent/toolchain, Jakarta APIs/descriptors and essential generators/providers. Preserve main-owned clients and models, repository contracts and business rules. Use the case/hearing, document and remaining repository groups as the default Java 25 conversion batches after the pilot, target platform and representative lifecycle are proved. Retire DeltaSpike only after reconciling all callers and proving replacement behaviour.

**Human work**

Progression and platform developers implement the pilot. Persistence, security and contract reviewers compare it with refreshed Java 17 evidence from Steps 01–04. Resolve Step 10 inputs with their publishers and prove representative PostgreSQL and WildFly/JTA outcomes with Step 12 before expanding the conversion.

**Acceptance evidence**

The target application builds with qualified released dependencies and identified unit and contract execution. Every remaining repository group has a reviewed disposition and preserves caller, lifecycle and business outcomes on the exact target stack. Retain effective dependencies, generated output and test evidence.

**Integration and release**

Start the draft pilot from current main now in parallel with Step 10. Use separate reviewable target batches after pilot proof. Final source acceptance requires the matched Step 12 configuration and Step 13 evidence.

### 12 Provide the matching runtime configuration

`migration/progression-12-wildfly40-runtime` in `cpp-aks-deploy`. Base is current main of cpp-aks-deploy, refreshed by its owner.

Prepare service-selectable deployment, image and configuration for Java 25, WildFly 40 and its matched Hibernate runtime. Link image/template owner PRs as needed and preserve existing-stack routing. Preparation starts after qualified Step 10 and completion uses a buildable Step 11 artifact.

**Human work**

Platform/operations owners inspect rendering and the actual target image, datasource, JNDI, identity, broker and startup behaviour. Confirm chart compatibility and environment scope.

**Acceptance evidence**

An authorised isolated test deployment of the Step 11 artifact records image digest and configuration and proves actual JVM/provider, CDI/security/JPA/JMS startup and representative PostgreSQL/WildFly JTA behaviour. Existing-stack services keep their intended routing.

**Integration and release**

Separate repository PR, coordinated with Step 11. A config merge or ready pod does not authorise production apply or establish business completion.

### 13 Prove the combined target release

`migration/progression-13-target-assurance` in `cpp-context-progression`. Base is migration/progression-11-java25-application-pilot until its parent changes merge.

Run the final application and all converted batches with matched Step 12 configuration. Compare with refreshed main and Java 17 baseline evidence from Steps 01–04. Cover PostgreSQL/Hibernate SQL, WildFly/JTA, access, repeated delivery, all required contracts including both MI classifiers, search, documents and notifications. Incorporate the assurance tests into the final target source and rerun the combined SHA.

**Human work**

Test lead reconciles discovered/executed/skipped test results; service/contract owners review business outcomes; performance and operations reviewers accept measured thresholds and rollback rehearsal.

**Acceptance evidence**

The exact final source SHA, image digest, configuration and qualified dependency manifest pass the outcome matrix, with current-main changes preserved. Reconcile discovered, executed and skipped results, including both MI classifiers, and accept measured performance and rollback evidence.

**Integration and release**

Stack assurance on the Step 11 branch while it is unmerged. Review and rerun the final combined SHA with matching runtime configuration before target integration or release.

### 14 Authorise rollout and record outcomes

Branch: `migration/progression-14-rollout-evidence` in `cpp-context-progression`. Base: accepted target source on main. Dependencies: 13.

Finalize deployment order, immutable release manifest, monitoring and recovery criteria; record bounded post-deployment outcome references after an authorised rollout. Keep payloads and logs out of Git.

Human work: Named release/operations owner authorises and executes deployment; service owner verifies representative business outcomes. Recovery/replay/data correction requires its own scoped authority.

Done when: Running artifact/configuration match the release; representative event/projection/query outcomes and operational thresholds pass. Follow approved stop/rollback criteria and retain results and follow-up ownership.

Merge and release rule: Documentation/evidence branch. Approval, deployment and business verification are human-owned actions, not consequences of creating or merging the branch.

## Evidence and limitations

This is a source-grounded delivery design with initial implementation drafts for Steps 01 to 04. Their four current PR verification pipelines are green; the executed suites, retained earlier failures and limits are recorded in [implementation status](implementation-status.md). Broader contract/business coverage, exact dependency/runtime manifests and PostgreSQL/current-WildFly/JTA qualification remain open. No PR has been merged to main and no deployment was performed. [The missing isolated test support is engineering work to add](test-support-next-steps.md); no ready-made gate is assumed.

Pinned main has 44 production repositories importing DeltaSpike out of 49 repository-named source files. The conversion groups reconcile to 19 case/hearing, 7 document and 18 workflow repositories. When a pilot is fully converted and accepted, record it once in its owning group. The current query pilot still uses DeltaSpike and is not counted as a completed conversion. All 44 need a final disposition.

Inspected framework source at 335bdfd3478039e89e67494928cdf2c457d02c3d contains an EntityManager producer, conditional active-transaction flush and a UTC global-default side effect. This is navigation evidence, not proof of the exact released/runtime implementation. Steps 04 and 08 must resolve the actual dependency and preserve required behaviour deliberately.

Follow-up source review located framework release-17.104.0 at e94e5b834b8a0f961f0768c9d0d39d5c7d3e8a45 and qualified its conditional post-listener flush path. [Invocation boundaries and remaining runtime evidence](implementation-status.md) distinguish that source result from actual resolved/deployed qualification.

- [Main POM](https://github.com/hmcts/cpp-context-progression/blob/ceebd3c81e899e1dfea4814de9016a49512d806a/pom.xml#L5)
- [Main pipeline](https://github.com/hmcts/cpp-context-progression/blob/ceebd3c81e899e1dfea4814de9016a49512d806a/azure-pipelines.yaml#L21)
- [Main persistence dependencies](https://github.com/hmcts/cpp-context-progression/blob/ceebd3c81e899e1dfea4814de9016a49512d806a/progression-viewstore/progression-viewstore-persistence/pom.xml#L14)
- [Existing direct javax JPA](https://github.com/hmcts/cpp-context-progression/blob/ceebd3c81e899e1dfea4814de9016a49512d806a/progression-viewstore/progression-viewstore-persistence/src/main/java/uk/gov/moj/cpp/prosecutioncase/persistence/repository/MaterialBulkRepository.java#L14)
- [Existing OPTIONAL contract](https://github.com/hmcts/cpp-context-progression/blob/ceebd3c81e899e1dfea4814de9016a49512d806a/progression-viewstore/progression-viewstore-persistence/src/main/java/uk/gov/moj/cpp/prosecutioncase/persistence/repository/MatchDefendantCaseHearingRepository.java#L25)
- [Target common parent Java requirements](https://github.com/hmcts/cpp-platform-maven-parent-pom/blob/25.104.4/pom.xml#L35)
- [WildFly 40 compatibility](https://www.wildfly.org/news/2026/05/21/WildFly-40-is-released/)
- [Hibernate 7.3 requirements](https://hibernate.org/orm/releases/7.3/)

## Conversion inventory

[All 44 DeltaSpike repositories and their owning branch step](repository-conversion-inventory.csv). The three groups reconcile without duplicates or omissions; the step 04 pilot remains counted within its owning group.

Framework navigation evidence for lifecycle qualification (the exact consumed artifact remains to be verified):

- [EntityManager producer](https://github.com/hmcts/cp-microservice-framework/blob/335bdfd3478039e89e67494928cdf2c457d02c3d/persistence/persistence-deltaspike/src/main/java/uk/gov/justice/services/persistence/EntityManagerProducer.java#L17)
- [Conditional flush registration](https://github.com/hmcts/cp-microservice-framework/blob/335bdfd3478039e89e67494928cdf2c457d02c3d/persistence/persistence-deltaspike/src/main/java/uk/gov/justice/services/persistence/EntityManagerFlushInterceptorProvider.java#L23)
