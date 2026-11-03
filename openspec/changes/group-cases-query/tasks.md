# Tasks: Add `progression.query.group-cases`

- [x] Add third `(mapping)` + schema/example JSON for `group-cases` to the RAML
- [x] Add `@Handles("progression.query.group-cases")` to `ProsecutionCaseQueryApi`
- [x] Add `getGroupCases()` to `ProsecutionCaseQuery`
- [x] Add `GroupCaseConverter` (new class)
- [x] Add access-control Drools rule for `progression.query.group-cases`
- [x] Add/extend unit tests
- [x] Add integration test

---

## Task detail

### Add third `(mapping)` + schema/example JSON for `group-cases` to the RAML

**File**: `progression-query/progression-query-api/src/raml/progression-query-api.raml`

In the `/prosecutioncases/group/{groupId}` resource (lines 272–298), add a
third `(mapping)` entry and a third response body entry — see `design.md` for
the exact diff.

New files (mirrored in both `progression-query-api` and
`progression-query-view` modules, matching the existing duplication pattern
for `group-member-cases`/`mastercase-details`):
- `src/raml/json/progression.query.group-cases.json` (example)
- `src/raml/json/schema/progression.query.group-cases.json` (schema — see
  `design.md` for the full draft-04 schema, using a local `caseSummary`
  definition rather than `$ref`-ing the shared `prosecutionCase.json#`)

After adding, run:
```bash
mvn generate-sources -pl progression-query/progression-query-api,progression-query/progression-query-view
```
to confirm the RAML/schema parse cleanly and any generated response POJO
(if the generator produces one for this mapping) builds.

---

### Add `@Handles("progression.query.group-cases")` to `ProsecutionCaseQueryApi`

**File**: `progression-query/progression-query-api/src/main/java/uk/gov/moj/cpp/progression/query/api/ProsecutionCaseQueryApi.java`

Add alongside the existing `geGroupMemberCases` handler (lines 87–90):

```java
@Handles("progression.query.group-cases")
public JsonEnvelope getGroupCases(final JsonEnvelope query) {
    return prosecutionCaseQuery.getGroupCases(query);
}
```

---

### Add `getGroupCases()` to `ProsecutionCaseQuery`

**File**: `progression-query/progression-query-view/src/main/java/uk/gov/moj/cpp/progression/query/ProsecutionCaseQuery.java`

Add alongside `getGroupMemberCases` (lines 231–257) — full method body in
`design.md`. Reuses `prosecutionCaseRepository.findByGroupId(groupId)` (same
call as the two existing handlers on this resource) with no post-fetch
filter removing the lead case.

Inject the new `GroupCaseConverter` alongside the existing
`jsonObjectToObjectConverter`/`stringToJsonObjectConverter` fields at the top
of the class.

---

### Add `GroupCaseConverter` (new class)

**File**: `progression-query/progression-query-view/src/main/java/uk/gov/moj/cpp/progression/query/utils/GroupCaseConverter.java`

New plain (non-DI) class — follows the `CaseLsmInfoConverter` pattern already
used in this package. Full implementation in `design.md`:
- `toLeadCase(List<ProsecutionCase>)` — filters `isGroupMaster()`, flattens to
  a `caseSummary` JSON object.
- `toCaseSummaries(List<ProsecutionCase>)` — flattens every case (including
  the lead) to a `JsonArrayBuilder` of `caseSummary` objects.
- `caseUrn` extraction: `caseURN` first, fallback `prosecutionAuthorityReference`
  (same precedence as `CaseLsmInfoQuery.extractCaseUrn`).
- `defendantName`: reuse
  `HearingAtAGlanceService.getDefendantName(personDefendant, legalEntityDefendant)`
  on `defendants[0]` — do not reimplement name-joining logic.
- `address`: pass `personDetails.getAddress()` straight through via
  `objectToJsonObjectConverter` — the generated `Address` type is already
  flat (`address1`-`address5`, `postcode`).

---

### Add access-control Drools rule for `progression.query.group-cases`

**File**: `progression-query/progression-query-api/src/main/resources/uk/gov/moj/cpp/progression/query/api/accesscontrol/query-access-control.drl`

Add a new rule mirroring the `group-member-cases` rule (lines 148–156), same
role list, action name changed to `progression.query.group-cases`. Full rule
text in `design.md`.

---

### Add/extend unit tests

**Files**:
- `progression-query/progression-query-api/src/test/java/uk/gov/moj/cpp/progression/query/api/ProsecutionCaseQueryApiTest.java`
  — add `shouldHandleGroupCasesQuery()`, mirroring
  `shouldHandleGroupMemberCasesQuery()` (lines 71–72, 206–213), asserting the
  new action dispatches to `prosecutionCaseQuery.getGroupCases()`.
- `progression-query/progression-query-view/src/test/java/uk/gov/moj/cpp/progression/query/view/ProsecutionCaseQueryViewTest.java`
  — add, mirroring `shouldFindGroupMemberCases()` (lines 2519–2544):
  - `shouldReturnGroupCasesIncludingLeadCase()` — group of 1 master + 2
    members; assert `cases` has 3 entries and `leadCase` matches the master.
  - `shouldUseFirstDefendantOnlyForGroupCaseSummary()` — case with 2
    defendants; assert the summary reflects `defendants[0]` only.
  - `shouldPreferCaseUrnOverProsecutionAuthorityReference()` — both fields
    populated on `prosecutionCaseIdentifier`; assert `caseURN` wins.
  - `shouldReturnEmptyCasesForUnknownGroup()` — `findByGroupId` returns an
    empty list; assert `{ "groupId": ..., "cases": [] }` with no `leadCase`
    key.
- New test class `progression-query/progression-query-view/src/test/java/uk/gov/moj/cpp/progression/query/utils/GroupCaseConverterTest.java`
  for the converter's flattening logic in isolation.

---

### Add integration test

**Actual implementation**: rather than a new `GroupCasesIT.java` (which would need to
rebuild the JMS/stub/group-setup scaffolding `CourtProceedingsInitiatedForGroupCasesIT`
already has), the `group-cases` assertions were added to the existing
`shouldInitiateCourtProceedingsForGroupCases()` test in
`progression-integration-test/src/test/java/uk/gov/moj/cpp/progression/CourtProceedingsInitiatedForGroupCasesIT.java`,
immediately after its existing `pollGroupMemberCases(...)` assertion — reusing the same
end-of-test group state (4 cases seeded, 2 removed, 2 remaining with a new master):

```java
// group-cases includes the lead case: 4 cases - 2 removed = 2
final Matcher[] groupCasesMatchers = new Matcher[]{
        withJsonPath("$.groupId", is(groupId)),
        withJsonPath("$.cases.length()", is(caseCount - 2)),
        withJsonPath("$.leadCase", notNullValue())
};

pollGroupCases(groupId, groupCasesMatchers);
```

A new `pollGroupCases(groupId, matchers)` helper was added to
`progression-integration-test/src/test/java/uk/gov/moj/cpp/progression/helper/PreAndPostConditionHelper.java`,
alongside `pollGroupMemberCases`, hitting
`GET /prosecutioncases/group/{groupId}` with
`Accept: application/vnd.progression.query.group-cases+json`.

This was verified to compile (`mvn test-compile -pl progression-integration-test -am`)
but **not executed** — running it requires the Docker-based integration environment
(`CPP_DOCKER_DIR`), which is not available in this session.

---

## Verification

```bash
mvn test -pl progression-query/progression-query-api,progression-query/progression-query-view
```

Confirm all new and existing tests pass, and that `group-member-cases` /
`mastercase-details` behaviour and tests are unaffected.

For the integration test:
```bash
CPP_DOCKER_DIR=/path/to/cpp-developers-docker ./runIntegrationTests.sh
```
