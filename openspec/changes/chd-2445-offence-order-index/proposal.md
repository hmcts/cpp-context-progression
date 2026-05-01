# CHD-2445: Include `orderIndex` in AAAG offence response

## Problem

The `progression.query.application.aaag+json` API returns offences inside `linkedCases` without the `orderIndex` field. This means consumers (e.g. the UI) cannot display the correct offence sequence number for multi-case applications. The AAAG page currently falls back to loop position, showing "1" for every first offence in each case regardless of its actual sequence.

The field exists in the view store (`offence.order_index`) and domain model (`Offence.getOrderIndex()`), but is absent from the AAAG JSON schema and therefore never mapped into the response.

## Jira

[CHD-2445](https://tools.hmcts.net/jira/browse/CHD-2445) — blocks [CHD-1929](https://tools.hmcts.net/jira/browse/CHD-1929)

## Scope

Backend only. This change makes `orderIndex` available in the API response. A dependent UI change in `cpp-ui-prosecution-casefile` (tracked separately) will consume it.

## Non-goals

- No change to how `orderIndex` is computed or stored — the view store already has the correct value.
- No change to other query endpoints.

## Approach

1. Add `orderIndex` (positiveInteger) to the `offence` definition in the AAAG JSON schema. This regenerates `Offences.java` with a `withOrderIndex()` builder method.
2. Map `offence.getOrderIndex()` in `ApplicationQueryView.getOffence()`.
3. Update the test fixture and assertions in `ApplicationQueryViewTest`.

## Spec

[aaag-linked-case-offences](../../specs/aaag-linked-case-offences/spec.md) — defines the full field contract and acceptance criteria this change implements.

## Dependencies

- Downstream: `cpp-ui-prosecution-casefile` UI change must follow after this is deployed.
