# Progression Java 25 first migration plan

Start the full application reactor from live Progression `main` at `f3dbf8c897e74302e5b3c54d22e4cd315c6b21de` on `migration/progression-11-java25-full-build`. Refresh the Java 17 comparison and resolve the real HMCTS platform and MI inputs in parallel. Before broad repository conversion, prove an actual Progression listener on the matched target with PostgreSQL and the framework JTA path. [Implementation status](implementation-status.md)

Draft PR 691 supplied the earlier upstream RelatedReference slice. Draft PR 690 supplied the prerequisite probe. Draft PRs 684–687 supplied the Java 17 comparison on their recorded baseline. Use these results to design the new checks and refresh current-main evidence for the new source revision. [Target evidence and open gates](java25-readiness.md)

## Outcome and scope

Preserve current main contracts, local Material and Listing adapters/models and business outcomes. Keep organisation fallback, contact corrections and date policy in separate reviews. The original repository inventory assigns 19 cases/hearings, seven documents and 18 remaining repositories. Refresh methods and callers against current main before confirming batch scope. [Original repository inventory](repository-conversion-inventory.csv)

## Branch and release rules

- Start Step 11 now. In parallel, refresh Steps 01–04 on current main and qualify the consumed HMCTS dependency, image, provider, pipeline and MI pins in Step 10.
- Prove the actual application listener, EntityManager lifecycle, post-listener flush, commit, rollback and repeated delivery on the matched Step 12 runtime before converting the wider repository population.
- Run Steps 05–07 as sibling Java 25 batches from the same qualified base. Combine and reconcile them in Step 08 before retiring DeltaSpike.
- Use the final source SHA, image digest and runtime configuration for one combined green pipeline, with executed MI and other contracts, source parity, business outcomes and rollback evidence. Step 14 authorises one coordinated cutover.
- Step 09 retains the historical option of a separate Java 17 release if the service owner identifies business value. Keep its scope and approval separate from the Java 25 route.
- Keep operational payloads and logs outside Git. Record branch bases, exact build and test evidence and human acceptance in the plan.

## Visual roadmap

[Open the visual roadmap](roadmap.md)

## Branch register

The table records each branch dependency. The runtime acceptance gate also compares the refreshed Java 17 evidence. Step 11 begins from live main while comparison and platform qualification run alongside it. Step 12 uses the qualified inputs and a buildable Step 11 artifact. Its listener gate controls entry to Steps 05–07.

| Step | Repository | Branch | Dependencies |
|---|---|---|---|
| 00 | cpp-context-progression | `plan/progression-java17-first-migration` | none |
| 01 | cpp-context-progression | `migration/progression-01-j17-baseline` | 00 |
| 02 | cpp-context-progression | `migration/progression-02-contract-characterisation` | 01 |
| 03 | cpp-context-progression | `migration/progression-03-j17-persistence-harness` | 02 |
| 04 | cpp-context-progression | `migration/progression-04-jpa-lifecycle` | 03 |
| 05 | cpp-context-progression | `migration/progression-05-jpa-cases-hearings` | 11, 12 |
| 06 | cpp-context-progression | `migration/progression-06-jpa-documents` | 11, 12 |
| 07 | cpp-context-progression | `migration/progression-07-jpa-registers-remaining` | 11, 12 |
| 08 | cpp-context-progression | `migration/progression-08-remove-deltaspike` | 05, 06, 07 |
| 09 optional | cpp-context-progression | `migration/progression-09-j17-release-evidence` | 04 |
| 10 | cpp-context-progression | `migration/progression-10-target-prerequisites` | none |
| 11 | cpp-context-progression | `migration/progression-11-java25-full-build` | none |
| 12 | cpp-aks-deploy | `migration/progression-12-wildfly40-runtime` | 10, 11 |
| 13 | cpp-context-progression | `migration/progression-13-target-assurance` | 08, 11, 12 |
| 14 | cpp-context-progression | `migration/progression-14-rollout-evidence` | 13 |

## Human work

| Role to assign | Human work and decision | Steps |
|---|---|---|
| Progression technical lead | Agree scope and assign owners; review method/caller inventory and each PR; preserve current main fixes. | 00 to 14 |
| Build and platform owners | Supply authorised artifact access, exact released dependency/source manifest, Java 25 agents, matched image/configuration and compatible contract-test harnesses. Resolve the consumed HMCTS platform pins and both MI test inputs. | 01 03 04 08 10 to 12 |
| Persistence and framework reviewer | Establish transaction/EntityManager ownership from actual artifacts; review lifecycle, cardinality, paging and SQL; approve retirement of framework support only after equivalence evidence. | 02 to 08 11 to 13 |
| Test lead and database support | Define representative PostgreSQL and container fixtures, execute meaningful assertions, reconcile discovery/execution/skips, compare baseline performance and retain results. | 01 to 08 11 to 13 |
| Service and business owner | Confirm preserved business outcomes and critical journeys. Decide genuine ambiguities; route business-rule changes through separate approval rather than bundling them into migration. | 00 02 09 11 13 14 |
| MI Listing and Material contract owners | Review schemas, request/response fixtures and model compatibility. Supply compatible consumer checks and verify the downstream outcomes in scope. | 02 10 11 13 |
| Release and operations owners | Approve environment scope and releases, rehearse rollback, agree monitoring and stop thresholds, execute authorised rollout and verify operational/business outcomes. | 09 12 to 14 |

Automation can assist with inventories, code and test drafts, report collection and rendering. Humans still own acceptance rules, independent reviews, artifact/environment access, interpretation of anomalies and release authority. These are proposed roles; named assignees, capacity and estimates have not been agreed.

## Step by step work packages

### 00 Agree the migration plan

`plan/progression-java17-first-migration` in `cpp-context-progression`. Base is origin/main at ceebd3c81e899e1dfea4814de9016a49512d806a.

Own the roadmap, branch register and acceptance evidence for the full Java 25 build from current main. Preserve current main behaviour and keep business fixes and model redesign in separate changes.

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

`migration/progression-03-j17-persistence-harness` in `cpp-context-progression`. Base is stacked on Step 02 at 60f7e8ea7a6102c7cde1fe5110e5907f41a9bfa4; predecessor fixes merged forward.

Introduce or select a Java 17-compatible javax JPA test harness that does not require DeltaSpike test-control. Add isolated PostgreSQL and container transaction coverage while keeping useful fast tests.

**Human work**

Test/platform owners provide approved fixtures and container access. Persistence reviewer checks that new tests exercise provider/container behaviour rather than mocks alone.

**Acceptance evidence**

The same baseline outcomes pass under the selected harness; PostgreSQL-specific queries execute on PostgreSQL and transaction tests are labelled separately from H2/resource-local checks.

**Integration and release**

Keep the Java 17 comparison on the current Javax and Hibernate stack. Qualify the Java 25 listener path separately in Step 12.

### 04 Prove explicit JPA lifecycle contracts

`migration/progression-04-jpa-lifecycle` in `cpp-context-progression`. Base is stacked on Step 03 at 0ba20fd468ff1bd8ce7ccfe0b160888e72665020; predecessor fixes merged forward.

Implement the smallest javax JPA repository seam/pilot on the existing runtime. Define persist/merge identity, assigned/composite keys, flush/delete and optional single-result behaviour before broad conversion.

**Human work**

Persistence and framework reviewers trace the exact released EntityManager producer, flush interceptor and transaction ownership. Confirm existing timezone behaviour and its ownership; preserve required policy deliberately.

**Acceptance evidence**

Pilot preserves managed-instance, detached-update/removal, zero/one/multiple-result, rollback and concurrency contracts. Required framework lifecycle/flush behaviour is retained. The selected injection approach demonstrably shares/joins the intended persistence context, with explicit ownership of flush, commit and rollback.

**Integration and release**

Keep existing JPA APIs and framework behaviour in the Java 17 comparison. Apply these lifecycle expectations to the actual Progression listener under the target runtime in Step 12.

### 05 Convert case hearing and defendant repositories

`migration/progression-05-jpa-cases-hearings` in `cpp-context-progression`. Base is common qualified Step 11 source on migration/progression-11-java25-full-build, with Step 12 listener evidence.

Convert the case, hearing and defendant repositories to explicit Jakarta JPA on Java 25. Refresh the original inventory from current main and preserve queries, signatures, exception/cardinality rules, ordering and paging.

**Human work**

Progression developer implements the group; independent persistence reviewer checks method and caller parity. Test lead runs hearing/defendant regression fixtures.

**Acceptance evidence**

Every in-scope repository/method has a disposition and tests for its actual key grain, excluded rows, optional multiplicity, ordering and paging. Relevant event/projection journeys pass.

**Integration and release**

Run this batch in parallel with 06 and 07 from the same qualified base. Prove the mixed repository behaviour used during development and combine the accepted batch in Step 08.

### 06 Convert document and material repositories

`migration/progression-06-jpa-documents` in `cpp-context-progression`. Base is common qualified Step 11 source on migration/progression-11-java25-full-build, with Step 12 listener evidence.

Convert document, material and index criteria repositories to explicit Jakarta JPA. Preserve one-to-many joins, fetch results, visibility predicates, case/application placement and material associations.

**Human work**

Progression and document/access reviewers verify the association grain and expected query outcomes. Test lead exercises document access, PDF/stream paths and removal flows.

**Acceptance evidence**

Multi-index fixtures, meaningful exclusions, JSON criteria where applicable, document ordering and returned associations match the baseline. Access and document journeys pass.

**Integration and release**

Run in parallel with 05 and 07. Review Material and access outcome parity and combine the accepted batch in Step 08.

### 07 Convert remaining repository groups

`migration/progression-07-jpa-registers-remaining` in `cpp-context-progression`. Base is common qualified Step 11 source on migration/progression-11-java25-full-build, with Step 12 listener evidence.

Convert registers, notifications, plea/COTR and remaining DeltaSpike users to explicit Jakarta JPA. Review the direct/JDBC repositories touched by shared changes and apply target Hibernate adaptations with equivalent results.

**Human work**

Persistence reviewer signs the complete method inventory. Register/notification journey owners confirm selection rules, dates and expected emitted/visible outcomes.

**Acceptance evidence**

Every allocated proxy usage has a disposition and latest-per-hearing, PostgreSQL SQL, filtering and notification paths preserve baseline results.

**Integration and release**

Run in parallel with 05 and 06. Combine the accepted batch in Step 08 and retain its tests in the final pipeline.

### 08 Retire DeltaSpike and preserve framework behaviour

`migration/progression-08-remove-deltaspike` in `cpp-context-progression`. Base is migration/progression-11-java25-full-build with accepted target batches 05–07.

Combine the three Java 25 repository batches and reconcile every Apache DeltaSpike caller, test and packaged dependency. Retire proxy and obsolete test-control dependencies once the target EntityManager lifecycle, active-transaction flush and timezone behaviour are proved.

**Human work**

Progression integrates the batches and resolves overlaps. Framework and persistence reviewers verify exact released-artifact responsibilities and replacement ownership. Test lead confirms the actual listener and repository outcomes across the combined source.

**Acceptance evidence**

Effective dependencies and packaged output contain the approved target persistence components. The combined application preserves lifecycle, flush, timezone, cardinality, SQL and business behaviour on the exact target. Every repository and remaining utility caller has a reviewed disposition.

**Integration and release**

Integrate the accepted batches into the full target build for Step 13 assurance and one coordinated cutover.

### 09 Retain the optional Java 17 release option

`migration/progression-09-j17-release-evidence` in `cpp-context-progression`. Base is separate Java 17 scope from accepted main only if the service owner requests business value.

Retain the historical Java 17 checkpoint as a separate option if the service owner identifies business value. Agree its independent scope, compare PostgreSQL/JTA, contracts and journeys and rehearse rollback.

**Human work**

Service and release owners decide whether to pursue a separate Java 17 release and own its acceptance and deployment authority.

**Acceptance evidence**

If separately commissioned, the agreed Java 17 scope passes its own lifecycle, contract, business and release evidence gates.

**Integration and release**

Optional separate work with its own acceptance. The full Java 25 build, conversion and cutover proceed independently.

### 10 Prepare the supported target platform

`migration/progression-10-target-prerequisites` in `cpp-context-progression`. Base is parallel on Step 01 at 010ca5bb632f9d47eff7f9c2bdd03252800561b5; refresh before integration.

Resolve the real HMCTS released parent/BOM, framework and core models, generators, build agent, deployed WildFly and Hibernate provider and both MI test classifiers. Pin the exact image, template, chart and test inputs and route changes through their owning repositories.

**Human work**

Platform owners supply tested versions and a compatibility manifest; operations provides a target test environment. Decide whether any temporary Java 17 Jakarta bridge is worth separate support; it is not assumed.

**Acceptance evidence**

The released inputs and actual HMCTS configuration support the target build and matched runtime. Pin both compatible MI artifacts and record their test inputs. Retain executed MI contract results from the combined Step 13 candidate.

**Integration and release**

Qualify real HMCTS platform and MI inputs in parallel with the full Step 11 reactor build and refreshed Java 17 comparison. Use the qualified inputs for Step 12.

### 11 Build the full Java 25 application reactor

`migration/progression-11-java25-full-build` in `cpp-context-progression`. Base is live Progression main at f3dbf8c897e74302e5b3c54d22e4cd315c6b21de.

Start the full Progression reactor from live main with the Java 25 and Jakarta source/build transition. Compile and package the application modules, generators and test artifacts with traceable dependencies. Preserve current main behaviour, local clients/models and test assertions. Make the compatibility changes needed for a buildable artifact and record remaining platform, application, persistence and contract blockers. Keep broader repository conversion in Steps 05–07.

**Human work**

Progression and platform developers implement the build from current main while Step 10 resolves the actual HMCTS pins and the Java 17 comparison is refreshed. Review compatibility changes and retain the build and test counts with exact source and toolchain.

**Acceptance evidence**

The full reactor builds and packages with the qualified Java 25 dependencies. Capture application artifacts, effective dependencies, generated output and identified unit and contract execution. Use the buildable artifact for Step 12 listener qualification.

**Integration and release**

Start immediately on live main and keep the build work in the draft until the runtime, repository and combined assurance gates pass. The final source and runtime move together at cutover.

### 12 Qualify the actual application listener and target runtime

`migration/progression-12-wildfly40-runtime` in `cpp-aks-deploy`. Base is current main of cpp-aks-deploy, refreshed by its owner.

Prepare service-selectable configuration for the real HMCTS Java 25/WildFly 40 target and its matched Hibernate provider. Use the buildable Step 11 Progression artifact to exercise a representative real application listener with PostgreSQL and the framework JTA path. Verify datasource, CDI, identity, JNDI, security and messaging against the consumed image and configuration.

**Human work**

Platform and operations owners pin and verify the target image/configuration. Progression and persistence reviewers choose an actual listener and prove EntityManager producer and transaction joining, pending-write visibility, post-listener flush, commit, rollback and repeated delivery. Compare the refreshed current-main baseline and preserve outcome parity.

**Acceptance evidence**

On the exact target image, the real listener uses the intended JTA persistence context and produces the expected committed outcomes. An independent read verifies commit and absence of partial writes after forced rollback. Retain the exact source, image, provider and configuration and close the gate before starting broad repository conversion.

**Integration and release**

Run the qualification on the buildable application and matched test runtime. Review the separate configuration PR and keep production source and runtime aligned for the cutover.

### 13 Prove the combined target release

`migration/progression-13-target-assurance` in `cpp-context-progression`. Base is migration/progression-11-java25-full-build with integrated Step 08 batches and matching Step 12 configuration.

Run the combined full reactor and release pipeline on the exact source, image, configuration and qualified platform pins. Compare refreshed current-main evidence, run actual PostgreSQL and WildFly/JTA listener paths, both MI classifiers and critical access, repeated-delivery, search, document and notification journeys.

**Human work**

Test lead reconciles discovered/executed/skipped test results; service/contract owners review business outcomes; performance and operations reviewers accept measured thresholds and rollback rehearsal.

**Acceptance evidence**

One combined candidate has a passing pipeline and identified executed assertions across the application, PostgreSQL/JTA, both MI contracts and business outcomes. Reconcile test populations, skips and current-main changes. Accept measured performance, monitoring and tested rollback for the exact artifact and configuration.

**Integration and release**

Use the combined target SHA and matching runtime for assurance. Authorise cutover only after the final pipeline and outcome gates pass.

### 14 Authorise one coordinated cutover and verify outcomes

`migration/progression-14-rollout-evidence` in `cpp-context-progression`. Base is accepted target source on main.

Record one coordinated Progression source/runtime cutover, its immutable release manifest, monitoring and recovery criteria. After the authorised rollout retain bounded evidence of actual deployed versions and business outcomes. Keep operational payloads and logs outside Git.

**Human work**

Named release/operations owner authorises and executes deployment; service owner verifies representative business outcomes. Recovery/replay/data correction requires its own scoped authority.

**Acceptance evidence**

Running artifact/configuration match the release; representative event/projection/query outcomes and operational thresholds pass. Follow approved stop/rollback criteria and retain results and follow-up ownership.

**Integration and release**

Documentation/evidence branch. Approval, deployment and business verification are human-owned actions, not consequences of creating or merging the branch.
