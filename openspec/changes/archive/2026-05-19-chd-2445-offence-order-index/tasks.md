# Tasks: CHD-2445 — Include `orderIndex` in AAAG offence response

- [x] Add `orderIndex` to the AAAG JSON schema
- [x] Map `orderIndex` in `ApplicationQueryView.getOffence()`
- [x] Update test fixture and assertions in `ApplicationQueryViewTest`
- [x] Add `orderIndex` integration test assertion to `ApplicationAtAGlanceIT`

---

## Task detail

### Add `orderIndex` to the AAAG JSON schema

**File**: `progression-domain/progression-domain-message/src/raml/json/schema/progression.query.application.aaag.json`

In the `"offence"` definition, add `orderIndex` after `count` (line 254):

```json
"orderIndex": {
  "$ref": "http://justice.gov.uk/core/courts/courtsDefinitions.json#/definitions/positiveInteger"
},
```

After this change, run:
```bash
mvn generate-sources -pl progression-domain/progression-domain-message
```
to regenerate `Offences.java` and confirm `withOrderIndex()` appears in `Offences.Builder`.

---

### Map `orderIndex` in `ApplicationQueryView.getOffence()`

**File**: `progression-query/progression-query-view/src/main/java/uk/gov/moj/cpp/progression/query/ApplicationQueryView.java`

In `getOffence()` (line 597), after `offenceBuilder.withCount(offence.getCount());`, add:

```java
ofNullable(offence.getOrderIndex()).ifPresent(offenceBuilder::withOrderIndex);
```

---

### Update test fixture and assertions in `ApplicationQueryViewTest`

**File**: `progression-query/progression-query-view/src/test/java/uk/gov/moj/cpp/progression/query/ApplicationQueryViewTest.java`

In `createOffences()` (line 1085), add `.withOrderIndex(2)` after `.withCount(5)`:

```java
.withCount(5)
.withOrderIndex(2)
```

Find the AAAG test that exercises `getCourtApplicationForApplicationAtAGlance` and add an assertion that `orderIndex` equals `2` in the returned offence JSON.

---

### Add `orderIndex` integration test assertion to `ApplicationAtAGlanceIT`

**File**: `progression-integration-test/src/test/java/uk/gov/moj/cpp/progression/ApplicationAtAGlanceIT.java`

In `verifyApplicationAtAGlance()`, alongside the existing `$.linkedCases[0]` assertions (after line ~309), add:

```java
withJsonPath("$.linkedCases[0].offences[0].orderIndex", equalTo(1)),
withJsonPath("$.linkedCases[0].offences[0].offenceCode", notNullValue())
```

The fixture (`progression.command.create-court-application-aaag.json`) already has `orderIndex: 1` in the offence — no fixture change is needed.

**No new imports required** — `equalTo` and `notNullValue` are already imported.

---

## Verification

```bash
mvn test -pl progression-query/progression-query-view -Dtest=ApplicationQueryViewTest
```

Confirm all tests pass and the AAAG test asserts `orderIndex` is present in the response.

For the integration test, run:
```bash
CPP_DOCKER_DIR=/path/to/cpp-developers-docker ./runIntegrationTests.sh
```
