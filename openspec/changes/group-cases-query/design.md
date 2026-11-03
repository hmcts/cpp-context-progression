# Design: `progression.query.group-cases`

## Current shape (unchanged queries, for reference)

```
GET /prosecutioncases/group/{groupId}
  (RAML (mapping) extension selects action by Accept media type)
        │
        ├─ application/vnd.progression.query.group-member-cases+json
        │     → ProsecutionCaseQueryApi.geGroupMemberCases()
        │         → ProsecutionCaseQuery.getGroupMemberCases()
        │             → findByGroupId(groupId)
        │               .filter(isGroupMember && !isGroupMaster)
        │             → { "prosecutionCases": [ <full ProsecutionCase>, ... ] }
        │
        └─ application/vnd.progression.query.mastercase-details+json
              → ProsecutionCaseQueryApi.getProsecutionMasterCaseDetails()
                  → ProsecutionCaseQuery.getProsecutionMasterCaseDetails()
                      → findByGroupId(groupId)
                        .filter(isGroupMaster).findFirst()
                      → { "masterCase": <full ProsecutionCase> }
```

Both handlers are `@ServiceComponent(Component.QUERY_API)` /
`@ServiceComponent(Component.QUERY_VIEW)` on `ProsecutionCaseQueryApi` /
`ProsecutionCaseQuery` respectively — a direct pass-through, no second
messaging hop. There is no `Group` table: `group_id` is a column on
`prosecution_case`, and master/member status is a boolean pair
(`isGroupMaster`, `isGroupMember`) embedded in each row's JSON `payload`.

## New shape

```
        └─ application/vnd.progression.query.group-cases+json   [NEW]
              → ProsecutionCaseQueryApi.getGroupCases()          [NEW]
                  → ProsecutionCaseQuery.getGroupCases()         [NEW]
                      → findByGroupId(groupId)      [same repo call, NO filter]
                          │
                          ├─ GroupCaseConverter.toSummary(each row) → flat case
                          │     (caseId, caseUrn, defendantName,
                          │      defendantDoB, address)
                          │
                          ├─ find row where isGroupMaster() == true
                          │     → leadCase  (reuse existing filter pattern)
                          │
                          └─ all flattened rows (including lead) → cases[]
                      → { "groupId": ..., "leadCase": {...}, "cases": [...] }
```

## RAML

**File**: `progression-query/progression-query-api/src/raml/progression-query-api.raml`

Add a third `(mapping)` to the existing resource (lines 272–298):

```raml
/prosecutioncases/group/{groupId}:
  uriParameters:
        groupId:
          description: ID of group cases
          type: string
  description: get member cases for group
  get:
    description: |
      ...
      (mapping):
          responseType: application/vnd.progression.query.group-member-cases+json
          name: progression.query.group-member-cases
      (mapping):
          responseType: application/vnd.progression.query.mastercase-details+json
          name: progression.query.mastercase-details
      (mapping):
          responseType: application/vnd.progression.query.group-cases+json
          name: progression.query.group-cases
      ...
    responses:
      200:
        description: OK
        body:
          application/vnd.progression.query.group-member-cases+json:
             example:  !include json/progression.query.group-member-cases.json
             schema:  !include json/schema/progression.query.group-member-cases.json
          application/vnd.progression.query.mastercase-details+json:
             example:  !include json/progression.query.mastercase-details.json
             schema:  !include json/schema/progression.query.mastercase-details.json
          application/vnd.progression.query.group-cases+json:
             example:  !include json/progression.query.group-cases.json
             schema:  !include json/schema/progression.query.group-cases.json
```

Mirror the same `!include` fragment pair in both
`progression-query/progression-query-api/src/raml/json(/schema)/` and
`progression-query/progression-query-view/src/raml/json(/schema)/` — matching
the existing pattern where identical copies of each fragment live in both
modules.

### New JSON schema

`progression.query.group-cases.json` (schema), modelled on the flat shape
rather than `$ref`-ing the shared heavyweight `prosecutionCase.json#` the
other two mappings use:

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "groupId": { "type": "string" },
    "leadCase": { "$ref": "#/definitions/caseSummary" },
    "cases": {
      "type": "array",
      "items": { "$ref": "#/definitions/caseSummary" }
    }
  },
  "required": ["groupId", "cases"],
  "definitions": {
    "caseSummary": {
      "type": "object",
      "properties": {
        "caseId": { "type": "string" },
        "caseUrn": { "type": "string" },
        "defendantName": { "type": "string" },
        "defendantDoB": { "type": "string", "format": "date" },
        "address": {
          "type": "object",
          "properties": {
            "address1": { "type": "string" },
            "address2": { "type": "string" },
            "address3": { "type": "string" },
            "address4": { "type": "string" },
            "address5": { "type": "string" },
            "postcode": { "type": "string" }
          }
        }
      },
      "required": ["caseId"]
    }
  }
}
```

`leadCase` is omitted (not null) from `required` — see "Empty group /
no-master-case behaviour" below.

## API layer

**File**: `progression-query/progression-query-api/src/main/java/uk/gov/moj/cpp/progression/query/api/ProsecutionCaseQueryApi.java`

Add alongside the existing handler at lines 87–90:

```java
@Handles("progression.query.group-cases")
public JsonEnvelope getGroupCases(final JsonEnvelope query) {
    return prosecutionCaseQuery.getGroupCases(query);
}
```

## Query-view layer

**File**: `progression-query/progression-query-view/src/main/java/uk/gov/moj/cpp/progression/query/ProsecutionCaseQuery.java`

Add alongside `getGroupMemberCases` (lines 231–257):

```java
@Handles("progression.query.group-cases")
public JsonEnvelope getGroupCases(final JsonEnvelope envelope) {
    final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
    final Optional<UUID> groupId = getUUID(envelope.payloadAsJsonObject(), GROUP_ID);
    jsonObjectBuilder.add(GROUP_ID, groupId.get().toString());
    try {
        final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseRepository.findByGroupId(groupId.get());
        final List<ProsecutionCase> prosecutionCases = prosecutionCaseEntities.stream()
                .map(o -> jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(o.getPayload()), ProsecutionCase.class))
                .collect(Collectors.toList());

        groupCaseConverter.toLeadCase(prosecutionCases)
                .ifPresent(leadCase -> jsonObjectBuilder.add(LEAD_CASE, leadCase));
        jsonObjectBuilder.add(CASES, groupCaseConverter.toCaseSummaries(prosecutionCases));
    } catch (final NoResultException e) {
        LOGGER.info("No group case found for groupId '{}'", groupId.get());
    }

    return JsonEnvelope.envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
}
```

`getMasterProsecutionCase` (lines 259–264) already contains the
"find the row where `isGroupMaster()`" filter this needs. Rather than
duplicating it a third time (it would then exist in
`getMasterProsecutionCase`, `getMemberProsecutionCases`'s inverse, and here),
move the lead-case lookup into `GroupCaseConverter` and have
`getMasterProsecutionCase` optionally delegate to it, or leave
`getMasterProsecutionCase` as-is and accept one small duplication — judgment
call at implementation time, not a design blocker either way.

## `GroupCaseConverter` (new, plain class — no DI)

**File**: `progression-query/progression-query-view/src/main/java/uk/gov/moj/cpp/progression/query/utils/GroupCaseConverter.java`

Follows the `CaseLsmInfoQuery` / `CaseLsmInfoConverter` pairing already used
in this module (Query handles repository access + top-level assembly; a
plain, non-DI Converter does per-case flattening via `JsonObjectBuilder` —
confirmed there is no MapStruct anywhere in this codebase).

```java
public class GroupCaseConverter {

    public Optional<JsonObject> toLeadCase(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .filter(ProsecutionCase::getIsGroupMaster)
                .findFirst()
                .map(this::toCaseSummary);
    }

    public JsonArrayBuilder toCaseSummaries(final List<ProsecutionCase> prosecutionCases) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        prosecutionCases.stream()
                .map(this::toCaseSummary)
                .forEach(jsonArrayBuilder::add);
        return jsonArrayBuilder;
    }

    private JsonObject toCaseSummary(final ProsecutionCase prosecutionCase) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(CASE_ID, prosecutionCase.getId().toString())
                .add(CASE_URN, extractCaseUrn(prosecutionCase));

        final Optional<Defendant> firstDefendant = prosecutionCase.getDefendants().stream().findFirst();
        firstDefendant.ifPresent(defendant -> {
            final Person personDetails = defendant.getPersonDefendant().getPersonDetails();
            builder.add(DEFENDANT_NAME, getDefendantName(defendant.getPersonDefendant(), defendant.getLegalEntityDefendant()));
            ofNullable(personDetails.getDateOfBirth()).ifPresent(dob -> builder.add(DEFENDANT_DOB, dob.toString()));
            ofNullable(personDetails.getAddress()).ifPresent(address -> builder.add(ADDRESS, objectToJsonObjectConverter.convert(address)));
        });

        return builder.build();
    }

    private String extractCaseUrn(final ProsecutionCase prosecutionCase) {
        // same precedence as CaseLsmInfoQuery.extractCaseUrn:
        // caseURN wins, falls back to prosecutionAuthorityReference
        final ProsecutionCaseIdentifier pci = prosecutionCase.getProsecutionCaseIdentifier();
        return pci.getCaseURN() != null ? pci.getCaseURN() : pci.getProsecutionAuthorityReference();
    }
}
```

`getDefendantName(personDefendant, legalEntityDefendant)` is
`HearingAtAGlanceService.getDefendantName` — a public static utility already
reused by three other classes (`ApplicationHearingQueryView`,
`SharedAllCourtDocumentsService`, `HearingAtAGlanceService` itself). Reuse it
directly rather than reimplementing name-joining logic; it also already
handles the legal-entity/organisation-defendant fallback.

`personDetails.getAddress()` returns the generated
`uk.gov.justice.core.courts.Address` type, which already has
`address1`–`address5` + `postcode` — no manual field-by-field flattening
needed, convert the object directly.

## Access control

**File**: `progression-query/progression-query-api/src/main/resources/uk/gov/moj/cpp/progression/query/api/accesscontrol/query-access-control.drl`

Add a rule mirroring the existing `group-member-cases` rule (lines 148–156),
same role list — reusing it rather than inventing a new one, since this is
the same case-group data viewed differently:

```
rule "Query - Access control for  progression.query.group-cases"
  when
    $outcome: Outcome();
    $action: Action(name == "progression.query.group-cases");
    eval(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups($action, "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
    "Youth Offending Service Admin", "Magistrates",  "District Judge", "Second Line Support", "Police Admin", "NCES", "Victims & Witness Care Admin", "Recorders", "Eject Case Group", "CPS" ,"Defence Lawyers", "Advocates"))
  then
    $outcome.setSuccess(true);
end
```

## Empty group / no-master-case behaviour

Following the existing `getGroupMemberCases` pattern (`NoResultException` →
log + return what's been built so far, no 404): if `groupId` matches no rows,
the response is `{ "groupId": "<id>", "cases": [] }` with no `leadCase` key
(schema does not require it). If rows exist but none has
`isGroupMaster == true` (a data-integrity edge case that should not occur in
practice, mirroring `getProsecutionMasterCaseDetails`'s existing handling of
the same situation), `leadCase` is likewise omitted rather than erroring.

## Test coverage to add

Following the precedent of `ProsecutionCaseQueryApiTest.shouldHandleGroupMemberCasesQuery()`
and `ProsecutionCaseQueryViewTest.shouldFindGroupMemberCases()`:

- `ProsecutionCaseQueryApiTest`: a `shouldHandleGroupCasesQuery()` test
  asserting the new action dispatches to `prosecutionCaseQuery.getGroupCases()`.
- `ProsecutionCaseQueryViewTest`: `shouldReturnGroupCasesIncludingLeadCase()` —
  group with one master + two member cases, asserting `cases[]` has 3 entries
  (not 2, unlike `group-member-cases`) and `leadCase` matches the master row.
- A case for the multi-defendant policy: case with 2 defendants, assert
  `defendantName`/`defendantDoB`/`address` reflect `defendants[0]` only.
- A case for `caseUrn` precedence: `prosecutionCaseIdentifier` with both
  `caseURN` and `prosecutionAuthorityReference` populated, assert `caseURN`
  wins.
- Empty-group case: `findByGroupId` returns empty list, assert
  `{ "groupId": ..., "cases": [] }` with no `leadCase` key.
