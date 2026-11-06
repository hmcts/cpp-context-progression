# Progression Java 17 first migration plan

Build from `main` pinned at `ceebd3c81e899e1dfea4814de9016a49512d806a`. This plan supersedes the earlier branch-comparison approach; that upgrade branch is not an implementation source. Steps 01 to 04 now have separate stacked draft PRs containing initial work. Their acceptance gates remain open; branch creation does not complete the steps. [See implementation status](implementation-status.md).

## Outcome and scope

Complete the baseline, contract tests and explicit JPA repository conversion on Java 17 and the existing `javax` stack. Preserve current runtime/provider, schema and business rules. Prove a releasable Java 17 checkpoint before the target source/runtime transition.

Main already owns Material and Listing adapters/models. Keep that work and test equivalence; do not add a new model/client redesign to this migration. Organisation fallback, contact corrections and business date-policy changes belong to separate normal defect/change work unless explicitly brought into scope.

## Platform boundary

WildFly 40 and Hibernate 7 support Java 17 upstream, but the independently inspected internal 25.x chain inherits parent 25.104.4 with Java [25,) enforcement and compiler source/target/release 25. The default route therefore completes all current-stack work on Java 17, then consumes a qualified Java 25/Jakarta EE 11/WildFly 40/Hibernate 7 platform together. A temporary Java 17 Jakarta bridge requires its own released and supported internal artifacts; do not assume one or override enforcement to manufacture it. Actual resolved JAR bytecode and runtime versions still require verification.

## Branch and release rules

- Create each implementation branch with an explicit base revision and prerequisite status. Steps 01 to 04 are stacked drafts; later names remain proposals. Review and accept predecessor evidence before accepting dependent work.
- Steps 01 to 09 branch from refreshed main after preceding changes merge. Stacked development is possible only with the dependent PR base made explicit. Preserve unrelated main changes.
- Step 10 is prerequisite documentation/coordination in Progression; platform owners provide separately scoped PRs in each owning repository as needed. Creating a branch does not publish those artifacts.
- Step 11 is the coherent target source/build branch. Step 12 is a separate deployment-repository PR. Neither establishes a safe production intermediate alone.
- Step 13 targets Step 11 while it is unmerged. Rebase the stacked branch when its parent merges; retest the final combined SHA/image/configuration. Step 14 is runbook/evidence work on accepted target main.
- Keep code, generated model changes, tests, configuration and human acceptance traceable. Avoid duplicate changes across branches. No force pushes, merges or deployments are implied by this plan.
- Keep sensitive fixtures, payloads and logs outside Git; commit approved synthetic tests, generic runbooks and bounded evidence references.

## Visual roadmap

[Open the visual roadmap](roadmap.md)

## Branch register

| Step | Repository | Proposed branch | Dependencies |
|---|---|---|---|
| 00 | cpp-context-progression | `plan/progression-java17-first-migration` | none |
| 01 | cpp-context-progression | `migration/progression-01-j17-baseline` | 00 |
| 02 | cpp-context-progression | `migration/progression-02-contract-characterisation` | 01 |
| 03 | cpp-context-progression | `migration/progression-03-j17-persistence-harness` | 02 |
| 04 | cpp-context-progression | `migration/progression-04-jpa-lifecycle` | 03 |
| 05 | cpp-context-progression | `migration/progression-05-jpa-cases-hearings` | 04 |
| 06 | cpp-context-progression | `migration/progression-06-jpa-documents` | 05 |
| 07 | cpp-context-progression | `migration/progression-07-jpa-registers-remaining` | 06 |
| 08 | cpp-context-progression | `migration/progression-08-remove-deltaspike` | 07 |
| 09 | cpp-context-progression | `migration/progression-09-j17-release-evidence` | 08 |
| 10 | cpp-context-progression | `migration/progression-10-target-prerequisites` | 01 |
| 11 | cpp-context-progression | `migration/progression-11-target-application` | 09, 10 |
| 12 | cpp-aks-deploy | `migration/progression-12-wildfly40-runtime` | 10 |
| 13 | cpp-context-progression | `migration/progression-13-target-assurance` | 11, 12 |
| 14 | cpp-context-progression | `migration/progression-14-rollout-evidence` | 13 |

## Human work

| Role to assign | Human work and decision | Steps |
|---|---|---|
| Progression technical lead | Agree scope and assign owners; review method/caller inventory and each PR; preserve current main fixes. | 00 to 14 |
| Build and platform owners | Supply authorised artifact access, exact released dependency/source manifest, Java 25 agents, matched image/configuration and compatible contract-test harnesses. Qualify any optional bridge explicitly. | 01 03 04 08 10 to 12 |
| Persistence and framework reviewer | Establish transaction/EntityManager ownership from actual artifacts; review lifecycle, cardinality, paging and SQL; approve retirement of framework support only after equivalence evidence. | 02 to 08 13 |
| Test lead and database support | Define representative PostgreSQL and container fixtures, execute meaningful assertions, reconcile discovery/execution/skips, compare baseline performance and retain results. | 01 to 09 13 |
| Service and business owner | Confirm preserved business outcomes and critical journeys. Decide genuine ambiguities; route business-rule changes through separate approval rather than bundling them into migration. | 00 02 09 13 14 |
| MI Listing and Material contract owners | Review schemas, request/response fixtures and model compatibility. Supply compatible consumer checks and verify the downstream outcomes in scope. | 02 10 13 |
| Release and operations owners | Approve environment scope and releases, rehearse rollback, agree monitoring and stop thresholds, execute authorised rollout and verify operational/business outcomes. | 09 12 to 14 |

Automation can assist with inventories, code and test drafts, report collection and rendering. Humans still own acceptance rules, independent reviews, artifact/environment access, interpretation of anomalies and release authority. These are proposed roles; named assignees, capacity and estimates have not been agreed.

## Step by step work packages

### 00 Agree the migration plan

Branch: `plan/progression-java17-first-migration` in `cpp-context-progression`. Base: origin/main at ceebd3c81e899e1dfea4814de9016a49512d806a. Dependencies: none.

Own the scope, branch register, undated roadmap, acceptance gates and evidence model. Start from current main; exclude unrelated business fixes and alternate model redesign.

Human work: Technical lead and service owner assign accountable people and agree the phased release policy. Platform, test and operations leads confirm dependencies and evidence expectations.

Done when: Each step has one scope, branch, proposed owner role and objective exit criteria; implementation and release remain pending.

Merge and release rule: Planning documents only. This is the only branch created in this task.

### 01 Reproduce the Java 17 baseline

Branch: `migration/progression-01-j17-baseline` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 00.

Record JDK/Maven, resolved parent/BOM, effective POM, dependencies, runtime image/configuration, discovered/executed/skipped tests and existing failures. Preserve all current main fixes.

Human work: Build owner supplies authorised private artifact access and a repeatable environment. Test lead distinguishes baseline defects, environmental failures and migration regressions; check owners triage failed gates.

Done when: Clean pinned baseline build and relevant journeys run in the owning environment, with retained logs and explicit failure disposition. No tests silently disappear.

Merge and release rule: Remain on Java 17 and the existing API/runtime stack. Evidence or safe build fixes only.

### 02 Characterise contracts and business outcomes

Branch: `migration/progression-02-contract-characterisation` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 01.

Pin zero/one/multiple repository results, ordering/paging, entity lifecycle and emitted events. Assert Material/PDF, Listing and MI contracts; main already owns the two local clients/models.

Human work: Service and contract owners confirm representative fixtures, expected event/projection outcomes and critical journeys. Business owner identifies any intentional difference for a separate change request.

Done when: Baseline assertions execute on Java 17, including ambiguous matches, missing/null values and meaningful exclusions. Required MI tests are discovered and executed with identified artifact versions.

Merge and release rule: No business-rule correction, client redesign or new fallback policy in this branch.

### 03 Prepare independent persistence test support

Branch: `migration/progression-03-j17-persistence-harness` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 02.

Introduce or select a Java 17-compatible javax JPA test harness that does not require DeltaSpike test-control. Add isolated PostgreSQL and container transaction coverage while keeping useful fast tests.

Human work: Test/platform owners provide approved fixtures and container access. Persistence reviewer checks that new tests exercise provider/container behaviour rather than mocks alone.

Done when: The same baseline outcomes pass under the selected harness; PostgreSQL-specific queries execute on PostgreSQL and transaction tests are labelled separately from H2/resource-local checks.

Merge and release rule: Keep production runtime, Hibernate, schemas and repository behaviour unchanged. Harness versions must remain compatible with the current stack.

### 04 Prove explicit JPA lifecycle contracts

Branch: `migration/progression-04-jpa-lifecycle` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 03.

Implement the smallest javax JPA repository seam/pilot on the existing runtime. Define persist/merge identity, assigned/composite keys, flush/delete and optional single-result behaviour before broad conversion.

Human work: Persistence and framework reviewers trace the exact released EntityManager producer, flush interceptor and transaction ownership. Confirm existing timezone behaviour and its ownership; preserve required policy deliberately.

Done when: Pilot preserves managed-instance, detached-update/removal, zero/one/multiple-result, rollback and concurrency contracts. Required framework lifecycle/flush behaviour is retained. The selected injection approach demonstrably shares/joins the intended persistence context, with explicit ownership of flush, commit and rollback.

Merge and release rule: Use APIs available on the existing JPA version; no Jakarta Persistence 3.2-only helpers. Retain the framework persistence artifact while needed.

### 05 Convert case hearing and defendant repositories

Branch: `migration/progression-05-jpa-cases-hearings` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 04.

Convert the case/hearing/defendant repository group to explicit javax JPA. Keep queries, signatures, exception/cardinality rules, ordering and paging equivalent.

Human work: Progression developer implements the group; independent persistence reviewer checks method and caller parity. Test lead runs hearing/defendant regression fixtures.

Done when: Every in-scope repository/method has a disposition and tests for its actual key grain, excluded rows, optional multiplicity, ordering and paging. Relevant event/projection journeys pass.

Merge and release rule: Merge and release independently on Java 17 once green. Leave remaining DeltaSpike repositories functioning.

### 06 Convert document and material repositories

Branch: `migration/progression-06-jpa-documents` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 05.

Convert document/material/index and related criteria repositories. Preserve one-to-many joins, fetch results, visibility predicates, case/application placement and material associations.

Human work: Progression and document/access reviewers verify the association grain and expected query outcomes. Test lead exercises document access, PDF/stream paths and removal flows.

Done when: Multi-index fixtures, meaningful exclusions, JSON criteria where applicable, document ordering and returned associations match the baseline. Access and document journeys pass.

Merge and release rule: Merge and release on Java 17. Do not use the conversion to redesign Material clients or access rules.

### 07 Convert remaining repository groups

Branch: `migration/progression-07-jpa-registers-remaining` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 06.

Convert registers, notifications, plea/COTR and remaining DeltaSpike users identified by the inventory. Review existing direct/JDBC repositories touched by any shared change.

Human work: Persistence reviewer signs the complete method inventory. Register/notification journey owners confirm selection rules, dates and expected emitted/visible outcomes.

Done when: All production DeltaSpike proxy usages are eliminated or explicitly accounted for; latest-per-hearing, native SQL, filtering and notification paths preserve baseline results.

Merge and release rule: Release on Java 17 after targeted and combined regression checks. Keep the current Hibernate query dialect; defer target-parser adaptation.

### 08 Retire DeltaSpike and preserve framework behaviour

Branch: `migration/progression-08-remove-deltaspike` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 07.

Remove Apache DeltaSpike and obsolete test-control dependencies only after all callers are converted. Replace/retain EntityManager lifecycle and active-transaction flush behaviour deliberately; trace transitive and packaged dependencies.

Human work: Framework owner validates the exact released artifact responsibilities. Progression reviewer approves replacement ownership; test lead proves commit/rollback and repeated-delivery effects. If a separate Java 17 framework artifact is required, add its owning repository change as a numbered prerequisite with its own branch before closing this step.

Done when: Effective dependency tree and packaged artifact contain no required-but-removed component and no unintended DeltaSpike runtime/test-control dependency. Lifecycle, flush and timezone outcomes remain agreed and tested.

Merge and release rule: Java 17 release gate. If the framework wrapper still supplies required behaviour, keep it and leave this gate open until a compatible replacement is available.

### 09 Accept and release the Java 17 checkpoint

Branch: `migration/progression-09-j17-release-evidence` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 08.

Record the cumulative Java 17 artifact, PostgreSQL/JTA, contract and end-to-end results, baseline performance comparison and rollback rehearsal. This branch holds runbook/evidence references, not operational data.

Human work: Service owner accepts outcome parity; test lead validates reports; operations/release owner authorises any deployment separately and confirms monitoring and stopping thresholds.

Done when: An independently releasable Java 17 version is proven with DeltaSpike removal complete and business events, projections and downstream outcomes reconciled. This gate remains open while Step 08 is unresolved.

Merge and release rule: Stop point with value delivered before Java 25. No target-runtime changes are required to close this checkpoint.

### 10 Prepare the supported target platform

Branch: `migration/progression-10-target-prerequisites` in `cpp-context-progression`. Base: main at Step 10 start after Step 01; refresh before integration. Dependencies: 01.

Record compatible released parent/BOM, framework/core models, generators, MI/other test artifacts, Java 25 agents and WildFly 40 image/configuration. Link owner PRs in platform/deployment repositories where changes are needed.

Human work: Platform owners supply tested versions and a compatibility manifest; operations provides a target test environment. Decide whether any temporary Java 17 Jakarta bridge is worth separate support; it is not assumed.

Done when: Identified artifacts and configuration can support the target service build/test cycle; image/template/chart revisions and required JNDI/datasource/messaging settings are recorded. No skipped contract checks are accepted by default.

Merge and release rule: Can progress in parallel with Java 17 work. This planning/evidence branch does not switch the service runtime or deploy anything.

### 11 Adapt the application to the target stack

Branch: `migration/progression-11-target-application` in `cpp-context-progression`. Base: latest main after preceding accepted steps. Dependencies: 09, 10.

Use the agreed Java 25 parent/toolchain, Jakarta APIs/descriptors, compatible generators/providers and Hibernate-required query adaptations. Keep the now-explicit repository architecture and main-owned clients/models.

Human work: Progression and platform developers implement only target compatibility. Persistence, API/security and contract reviewers assess diffs; test lead checks generated/packaged output and no loss of assertions.

Done when: Target application builds with all required contract/unit tests on Java 25; repository contracts and business rules remain unchanged. Exact effective dependencies and generated output are retained.

Merge and release rule: Keep the PR open until matching runtime Step 12 and integrated Step 13 are green. Treat coupled API/dependency changes as one coherent source transition, not unsupported intermediate main releases.

### 12 Provide the matching runtime configuration

Branch: `migration/progression-12-wildfly40-runtime` in `cpp-aks-deploy`. Base: current main of cpp-aks-deploy, refreshed by its owner. Dependencies: 10.

Prepare the service-selectable deployment/image/configuration changes for Java 25, WildFly 40 and its matched Hibernate runtime. Link image/template owner PRs if required; preserve existing-stack deployments. Preparation starts after Step 10; completion requires a buildable Step 11 artifact.

Human work: Platform/operations owners inspect rendering and the actual target image, datasource, JNDI, identity, broker and startup behaviour. Confirm chart compatibility and environment scope.

Done when: An authorised test deployment of the Step 11 artifact resolves CDI/security/JPA/JMS components using the recorded runtime image/digest and configuration. Existing-stack services remain on their intended configuration.

Merge and release rule: Separate repository PR, coordinated with Step 11. A config merge or ready pod does not authorise production apply or establish business completion.

### 13 Prove the combined target release

Branch: `migration/progression-13-target-assurance` in `cpp-context-progression`. Base: migration/progression-11-target-application until its parent changes merge. Dependencies: 11, 12.

Add final target-specific tests/evidence and run the integrated source with matched deployment configuration. Cover PostgreSQL/Hibernate SQL, WildFly/JTA, identity/access, repeated delivery, contracts, search, documents, notifications and MI outcomes. Dependencies here mean built source and matching test configuration, not already merged PRs; integrate these tests into Step 11 and rerun its final combined SHA.

Human work: Test lead reconciles discovered/executed/skipped test results; service/contract owners review business outcomes; performance and operations reviewers accept measured thresholds and rollback rehearsal.

Done when: The final combined SHA/image/configuration passes the agreed outcome matrix, including main fixes retained throughout. Open issues have explicit dispositions; no passing status is substituted for absent execution.

Merge and release rule: Stacked tests-only PR targets Step 11 while it is unmerged; rebase if its parent merges. Final source merge/release waits for the combined gate.

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
