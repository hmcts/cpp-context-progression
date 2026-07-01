# PCR defendantId Propagation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Propagate the case-specific `defendantId` (distinct from `masterDefendantId`) into the `PcrEventPayload` sent from `PrisonCourtRegisterEventProcessor` to AMP, so CPR/downstream matching can use the reliable, case-scoped identifier instead of the ambiguous cross-case `masterDefendantId`.

**Architecture:** `HearingResultsDocumentSubscriptionPCRMapper` builds `PcrEventPayload` from the inbound `PrisonCourtRegisterGeneratedV2` event, which only carries `masterDefendantId` (the shared core-domain PCR schema has no case-specific defendant id and cannot be extended without a coredomain-wide version bump). Since `PcrEventPayload`/`PcrEventPayloadDefendantCases` are progression's own hand-written DTOs, not schema-generated, `defendantId` can be resolved locally and added there without touching any schema. Resolution happens per case: `ProsecutionCaseRepository` gains a native query that looks up a case's own defendant list (already stored as JSON in `prosecution_case.payload`) filtered by both `caseURN` and `masterDefendantId` together, returning the case-specific `defendantId`. This avoids the defect class found in the AMP-636 investigation, where resolving by `masterDefendantId` alone returns multiple `defendantId`s for a person with more than one case.

**Tech Stack:** Java 17, JUnit 5, Mockito, Lombok (`@Builder`), Deltaspike Data (`@Query`, native SQL) in `progression-viewstore-persistence`.

## Global Constraints

- Do not modify `cpp-context-azure-legalaidagency` or `cpp-context-hearing-nows` — out of scope. The companion HRDS-side fix is tracked separately at `cpp-context-hearing-nows/docs/superpowers/plans/2026-07-01-hrds-defendantid-propagation.md`.
- Do not modify any RAML/JSON schema, and do not touch `criminal-court-public-model`/coredomain — `PcrEventPayload`, `PcrEventPayloadDefendant`, and `PcrEventPayloadDefendantCases` are progression's own hand-written DTOs (not schema-generated), so this fix needs no coredomain.version bump.
- Resolve strictly by `(caseURN AND masterDefendantId)` together, never `masterDefendantId` alone across cases — a bare `masterDefendantId` lookup can return multiple `defendantId`s for a person with more than one case (the exact defect found in the AMP-636 investigation).
- Fall back to `masterDefendantId` (as a string) when no case-specific match is found — this is safe because `Defendant.id == masterDefendantId` before any case-matching drift occurs for that defendant.
- No `pom.xml` changes anywhere in this plan — `progression-viewstore-persistence` (home of `ProsecutionCaseRepository`) is already a compile-scope dependency of `progression-event-processor`.

---

## File Structure

- Modify: `progression-viewstore/progression-viewstore-persistence/src/main/java/uk/gov/moj/cpp/prosecutioncase/persistence/repository/ProsecutionCaseRepository.java` — add the native query that resolves a case-specific `defendantId` from a case's own payload, scoped by `caseURN` + `masterDefendantId`.
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/dto/PcrEventPayloadDefendantCases.java` — add the `defendantId` field to the outbound per-case DTO.
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapper.java` — inject `ProsecutionCaseRepository`, resolve `defendantId` per case, set it on each `PcrEventPayloadDefendantCases`.
- Modify: `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java` — add tests for the resolved and fallback cases.

---

### Task 1: Add `defendantId` to `PcrEventPayloadDefendantCases`

**Files:**
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/dto/PcrEventPayloadDefendantCases.java`
- Test: `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java`

**Interfaces:**
- Produces: `PcrEventPayloadDefendantCases.builder().urn(String).defendantId(String).build()`, `PcrEventPayloadDefendantCases.getDefendantId(): String` — consumed by Task 3.

- [ ] **Step 1: Write the failing test**

Add this test to `HearingResultsDocumentSubscriptionPCRMapperTest.java` (inside the existing test class body):

```java
@Test
void defendantCasesBuilderShouldExposeDefendantId() {
    final PcrEventPayloadDefendantCases defendantCase = PcrEventPayloadDefendantCases.builder()
            .urn("SJ54CYRNYB")
            .defendantId("11111111-1111-1111-1111-111111111111")
            .build();

    assertThat(defendantCase.getUrn(), equalTo("SJ54CYRNYB"));
    assertThat(defendantCase.getDefendantId(), equalTo("11111111-1111-1111-1111-111111111111"));
}
```

`PcrEventPayloadDefendantCases`, `assertThat`, and `equalTo` are already imported in this test file — no new imports needed for this step.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl progression-event/progression-event-processor test -Dtest=HearingResultsDocumentSubscriptionPCRMapperTest#defendantCasesBuilderShouldExposeDefendantId`
Expected: FAIL to compile — `cannot find symbol: method defendantId(String)` (Lombok has not generated a builder method for a field that does not exist yet).

- [ ] **Step 3: Add the field**

In `PcrEventPayloadDefendantCases.java`, change:

```java
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
// Generated and copied from AMP
public class PcrEventPayloadDefendantCases {
    private String urn;
}
```

to:

```java
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
// Generated and copied from AMP
public class PcrEventPayloadDefendantCases {
    private String urn;
    private String defendantId;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl progression-event/progression-event-processor test -Dtest=HearingResultsDocumentSubscriptionPCRMapperTest#defendantCasesBuilderShouldExposeDefendantId`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/dto/PcrEventPayloadDefendantCases.java progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java
git commit -m "feat(pcr): add defendantId field to PcrEventPayloadDefendantCases"
```

---

### Task 2: Add the case-scoped `defendantId` lookup to `ProsecutionCaseRepository`

**Files:**
- Modify: `progression-viewstore/progression-viewstore-persistence/src/main/java/uk/gov/moj/cpp/prosecutioncase/persistence/repository/ProsecutionCaseRepository.java`

**Interfaces:**
- Produces: `ProsecutionCaseRepository.findDefendantIdByCaseUrnAndMasterDefendantId(String urn, String masterDefendantId): Optional<String>` — consumed by Task 3.

`prosecution_case.payload` stores each case as a JSON blob with a nested `defendants` array, each defendant carrying its own `id` (the case-specific `defendantId`) and `masterDefendantId`. There is no typed JPA mapping for this nested structure — the codebase's established pattern for this kind of lookup (see the existing `findInactiveMigratedCaseSummaries` native query in this same file) is a native `LATERAL jsonb_array_elements` query directly against the `payload` column, scoped by `p.id IN (:caseIds)` or similar. This task follows that same established pattern, scoped instead by `caseURN` + `masterDefendantId`.

This repository method has no dedicated unit test in this codebase — Deltaspike-generated implementations of native `@Query`-annotated interface methods are not unit-testable without a real database connection in this project's existing test setup (confirmed: none of the other native `@Query` methods in this file, e.g. `findInactiveMigratedCaseSummaries`, have a dedicated unit test either). Correctness of this method's *usage* is covered by the mapper-level tests in Task 3, which mock this method's return value. If DB-level validation of the native SQL itself is wanted, that would need a new integration test using this project's existing IT harness — out of scope for this plan (see "Out of scope / follow-up" below).

- [ ] **Step 1: Add the query method**

In `ProsecutionCaseRepository.java`, add this method to the interface, after the existing `findInactiveMigratedCaseSummaries` method:

```java
    @Query(value = """
    SELECT def ->> 'id'
    FROM prosecution_case p,
    LATERAL jsonb_array_elements(CAST(p.payload AS jsonb) -> 'defendants') AS def
    WHERE CAST(p.payload AS jsonb) -> 'prosecutionCaseIdentifier' ->> 'caseURN' = :urn
    AND def ->> 'masterDefendantId' = :masterDefendantId
    """, isNative = true, singleResult = SingleResultType.OPTIONAL)
    Optional<String> findDefendantIdByCaseUrnAndMasterDefendantId(@QueryParam("urn") String urn,
                                                                    @QueryParam("masterDefendantId") String masterDefendantId);
```

Add these imports at the top of the file (alongside the existing `java.util.List`/`java.util.UUID` imports and `org.apache.deltaspike.data.api.*` imports):

```java
import java.util.Optional;

import org.apache.deltaspike.data.api.SingleResultType;
```

- [ ] **Step 2: Compile to verify the new method is valid**

Run: `mvn -pl progression-viewstore/progression-viewstore-persistence compile`
Expected: BUILD SUCCESS — this step only confirms the interface compiles; it is not itself a passing/failing unit test (see the note above on why this method has no dedicated unit test).

- [ ] **Step 3: Commit**

```bash
git add progression-viewstore/progression-viewstore-persistence/src/main/java/uk/gov/moj/cpp/prosecutioncase/persistence/repository/ProsecutionCaseRepository.java
git commit -m "feat(pcr): add case-scoped defendantId lookup to ProsecutionCaseRepository"
```

---

### Task 3: Resolve `defendantId` per case in `HearingResultsDocumentSubscriptionPCRMapper`

**Files:**
- Modify: `progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapper.java`
- Modify: `progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java`

**Interfaces:**
- Consumes: `PcrEventPayloadDefendantCases.builder().defendantId(String)` (Task 1). `ProsecutionCaseRepository.findDefendantIdByCaseUrnAndMasterDefendantId(String, String): Optional<String>` (Task 2).
- Produces: `HearingResultsDocumentSubscriptionPCRMapper` now requires a `ProsecutionCaseRepository` to be injected — no change to `mapPcrForhearingResultsDocument`'s own signature, so `PrisonCourtRegisterEventProcessor` (its only caller) needs no changes, since CDI `@Inject` resolves the new dependency automatically.

- [ ] **Step 1: Write the failing test — case-specific defendantId resolved**

Add this test to `HearingResultsDocumentSubscriptionPCRMapperTest.java`:

```java
@Test
void mapperShouldResolveCaseScopedDefendantIdFromProsecutionCasePayload() {
    final String masterDefendantId = "d78e8cac-991c-43fa-86a7-8fc6b857308a";
    final String resolvedDefendantId = "11111111-1111-1111-1111-111111111111";
    when(prosecutionCaseRepository.findDefendantIdByCaseUrnAndMasterDefendantId("SJ54CYRNYB", masterDefendantId))
            .thenReturn(Optional.of(resolvedDefendantId));

    final PcrEventPayload payload = mapper.mapPcrForhearingResultsDocument(
            pcr, "wandsworth@example.com", Instant.parse("2024-10-01T10:00:00Z"), null);

    final PcrEventPayloadDefendantCases case0 = payload.getDefendant().getCases().get(0);
    assertThat(case0.getDefendantId(), equalTo(resolvedDefendantId));
}

@Test
void mapperShouldFallBackToMasterDefendantIdWhenNoCaseMatchFound() {
    when(prosecutionCaseRepository.findDefendantIdByCaseUrnAndMasterDefendantId("SJ54CYRNYB", "d78e8cac-991c-43fa-86a7-8fc6b857308a"))
            .thenReturn(Optional.empty());

    final PcrEventPayload payload = mapper.mapPcrForhearingResultsDocument(
            pcr, "wandsworth@example.com", Instant.parse("2024-10-01T10:00:00Z"), null);

    final PcrEventPayloadDefendantCases case0 = payload.getDefendant().getCases().get(0);
    assertThat(case0.getDefendantId(), equalTo("d78e8cac-991c-43fa-86a7-8fc6b857308a"));
}
```

Add the `@Mock ProsecutionCaseRepository prosecutionCaseRepository;` field to the test class, alongside the existing `@InjectMocks HearingResultsDocumentSubscriptionPCRMapper mapper;` field:

```java
@Mock
ProsecutionCaseRepository prosecutionCaseRepository;

@InjectMocks
HearingResultsDocumentSubscriptionPCRMapper mapper;
```

Add these imports to `HearingResultsDocumentSubscriptionPCRMapperTest.java`:

```java
import org.mockito.Mock;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.Optional;

import static org.mockito.Mockito.when;
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl progression-event/progression-event-processor test -Dtest=HearingResultsDocumentSubscriptionPCRMapperTest`
Expected: FAIL — `mapperShouldResolveCaseScopedDefendantIdFromProsecutionCasePayload` and `mapperShouldFallBackToMasterDefendantIdWhenNoCaseMatchFound` fail with `case0.getDefendantId()` returning `null` (the mapper does not resolve or set `defendantId` yet), and `UnnecessaryStubbingException` from Mockito's strict stubs on the `when(prosecutionCaseRepository...)` calls since nothing in the mapper invokes that method yet.

- [ ] **Step 3: Wire the resolution into the mapper**

In `HearingResultsDocumentSubscriptionPCRMapper.java`, add these imports:

```java
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.UUID;

import javax.inject.Inject;
```

Add the injected field, as the first member of the class:

```java
@Inject
private ProsecutionCaseRepository prosecutionCaseRepository;
```

Change `mapCases` from:

```java
private List<PcrEventPayloadDefendantCases> mapCases(PrisonCourtRegisterDefendant pcrDefendant) {
    return pcrDefendant.getProsecutionCasesOrApplications() == null
            ? Collections.emptyList()
            : pcrDefendant.getProsecutionCasesOrApplications().stream()
            .map(c ->
                    PcrEventPayloadDefendantCases.builder().urn(c.getCaseOrApplicationReference()).build()).toList();
}
```

to:

```java
private List<PcrEventPayloadDefendantCases> mapCases(PrisonCourtRegisterDefendant pcrDefendant) {
    if (pcrDefendant.getProsecutionCasesOrApplications() == null) {
        return Collections.emptyList();
    }
    final UUID masterDefendantId = pcrDefendant.getMasterDefendantId();
    return pcrDefendant.getProsecutionCasesOrApplications().stream()
            .map(c -> PcrEventPayloadDefendantCases.builder()
                    .urn(c.getCaseOrApplicationReference())
                    .defendantId(resolveDefendantId(c.getCaseOrApplicationReference(), masterDefendantId))
                    .build())
            .toList();
}

private String resolveDefendantId(String urn, UUID masterDefendantId) {
    if (masterDefendantId == null) {
        return null;
    }
    return prosecutionCaseRepository
            .findDefendantIdByCaseUrnAndMasterDefendantId(urn, masterDefendantId.toString())
            .orElse(masterDefendantId.toString());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl progression-event/progression-event-processor test -Dtest=HearingResultsDocumentSubscriptionPCRMapperTest`
Expected: PASS (all tests, including the two new ones and the two written in Task 1)

- [ ] **Step 5: Run the full module test suite**

Run: `mvn -pl progression-event/progression-event-processor test`
Expected: PASS, no regressions in `PrisonCourtRegisterEventProcessorTest` or any other test class in this module — `PrisonCourtRegisterEventProcessor` calls `mapPcrForhearingResultsDocument` with the same signature as before, so it needs no changes.

- [ ] **Step 6: Commit**

```bash
git add progression-event/progression-event-processor/src/main/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapper.java progression-event/progression-event-processor/src/test/java/uk/gov/moj/cpp/progression/service/amp/mappers/HearingResultsDocumentSubscriptionPCRMapperTest.java
git commit -m "feat(pcr): resolve case-scoped defendantId and propagate to PcrEventPayload"
```

---

## Out of scope / follow-up

- `ProsecutionCaseRepository.findDefendantIdByCaseUrnAndMasterDefendantId` (Task 2) has no dedicated DB-level integration test in this plan, consistent with the existing pattern for other native `@Query` methods in the same interface. Adding one via this project's existing IT harness is a reasonable fast-follow.
- `cpp-context-azure-legalaidagency` (the caller that builds the original `PrisonCourtRegisterDocumentRequest`) and `cpp-context-hearing-nows` (the separate HRDS/AMP notification channel) are untouched by design — this plan only affects `cpp-context-progression`'s own outbound `PcrEventPayload` construction. The HRDS-side companion fix is a separate plan/PR in `cpp-context-hearing-nows`.
