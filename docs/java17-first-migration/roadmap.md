# Visual migration roadmap

This is an evidence-gated sequence, not a calendar schedule. Every numbered step maps to a separate branch in the register.

```mermaid
flowchart TB
    P00["00 Agree the plan"] --> S01
    subgraph J17A[Java 17 preparation]
      S01["01 Reproduce baseline"] --> S02["02 Characterise contracts"] --> S03["03 Prepare test support"] --> S04["04 Prove JPA lifecycle"]
    end
    subgraph J17B[Java 17 persistence]
      S05["05 Case and hearing JPA"] --> S06["06 Document JPA"] --> S07["07 Remaining JPA"] --> S08["08 Retire DeltaSpike"]
    end
    S04 --> S05
    S08 --> S09["09 Accept Java 17 release checkpoint"]
    S01 -. parallel platform preparation .-> S10["10 Qualify target dependencies"]
    S09 --> S11["11 Target application Java 25 and Jakarta"]
    S10 --> S11
    S10 --> S12["12 Matching WildFly 40 and Hibernate runtime"]
    S11 --> S13["13 Combined target assurance"]
    S12 --> S13
    S13 --> S14["14 Authorise rollout and verify outcomes"]
```

[Full visual roadmap](roadmap.svg)

Steps 01 to 09 preserve Java 17 and the existing enterprise APIs, Hibernate provider and runtime. Step 10 can progress in parallel. Steps 11 and 12 are separately reviewed source/configuration changes but require coordinated target validation; no unsupported intermediate production stack is assumed. Step 13 is a stacked test/evidence branch on Step 11 until parent changes merge.
