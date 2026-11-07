# Visual migration roadmap

The Java 25 application pilot starts from current `main` while the platform owners resolve Step 10. Steps 01–04 supply the refreshed Java 17 comparison. Steps 05–09 are an optional intermediate release. Arrows show dependencies and acceptance gates without calendar dates.

```mermaid
flowchart TB
    P00["00 Agree the plan"] --> S01
    subgraph BASE[Refresh the Java 17 comparison]
      S01["01 Reproduce baseline"] --> S02["02 Characterise contracts"] --> S03["03 Prepare test support"] --> S04["04 Prove JPA lifecycle"]
    end
    subgraph OPTIONAL[Optional Java 17 release lane]
      S05["05 Case and hearing JPA"] --> S06["06 Document JPA"] --> S07["07 Remaining JPA"] --> S08["08 Retire DeltaSpike"] --> S09["09 Optional Java 17 release checkpoint"]
    end
    S04 -. if selected .-> S05
    S01 -. original probe base .-> S10["10 Qualify target inputs / PR 690"]
    MAIN["Progression main"] --> S11["11 Java 25 application pilot / draft PR 691"]
    S10 -->|qualified inputs| S12["12 Prepare matching WildFly 40 and Hibernate runtime"]
    S11 -->|buildable pilot artifact| S12
    S04 --> G["Compare refreshed baseline and prove target lifecycle and contracts"]
    S12 --> G
    G --> B["11 Expand into reviewable Java 25 repository batches"]
    B --> S13["13 Combined target assurance on exact source and runtime"]
    S12 --> S13
    S13 --> S14["14 Authorise rollout and verify outcomes"]
```

[Open the SVG roadmap](roadmap.svg)

Step 11 can begin while the baseline refresh and Step 10 are in progress. Before broader conversion, qualify the selected target dependencies and prove representative PostgreSQL and WildFly/JTA lifecycle and contract outcomes with a buildable pilot. Convert case/hearing, document and remaining repository groups on Java 25 by default. If the optional Java 17 route is selected, carry accepted changes into the target without converting the same methods twice.

The first Step 11 slice passed locally on upstream WildFly 40 and PostgreSQL.
[Draft PR 691](https://github.com/hmcts/cpp-context-progression/pull/691) ·
[Executed evidence and open gates](java25-readiness.md#step-11-first-application-slice)

Step 12 prepares after qualified Step 10 and completes with a buildable Step 11 artifact. Step 13 can stack tests on Step 11 while it is unmerged. Accept the final combined source, image, configuration, executed MI and other contracts, measured performance and rollback evidence before integration and release. [Branch register](branch-register.csv) · [Target evidence and open gates](java25-readiness.md)
