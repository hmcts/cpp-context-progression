# Spec: AAAG Linked Case Offences

## Capability

The Application At A Glance (AAAG) query returns offences associated with an application across one or more linked prosecution cases. Each offence must carry enough data for the UI to display the correct sequence number, charge details, results, and restrictions — without needing to call any secondary endpoint.

**Query**: `progression.query.application.aaag`  
**Response type**: `application/vnd.progression.query.application.aaag+json`  
**Relevant path**: `response.linkedCases[].offences[]`

---

## Data Lineage

```
PostgreSQL view store
  offence.order_index       (int, NOT NULL)
  offence.offence_code      (varchar)
  offence.offence_title     (varchar)
  ...
    ↓ loaded into domain model
  uk.gov.justice.core.courts.Offence
    .getOrderIndex()        → Integer
    .getOffenceCode()       → String
    ...
      ↓ mapped by ApplicationQueryView.getOffence()
      ↓ into generated DTO (from progression.query.application.aaag.json schema)
  uk.gov.justice.courts.progression.query.Offences
    → serialised into JSON response
```

The view store is the authoritative source. The domain object and DTO must faithfully relay the value without transformation.

---

## Field Contract

Each offence object in `linkedCases[].offences[]` MUST include the following fields:

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | UUID | No | Offence identifier |
| `offenceCode` | string | No | Statutory offence code |
| `offenceTitle` | string | No | English offence title |
| `offenceTitleWelsh` | string | Yes | Welsh offence title |
| `offenceLegislation` | string | No | Legislation text |
| `offenceLegislationWelsh` | string | Yes | Welsh legislation text |
| `wording` | string | No | Specific wording for this charge |
| `wordingWelsh` | string | Yes | Welsh wording |
| `startDate` | date (yyyy-MM-dd) | No | Offence start date |
| `endDate` | date (yyyy-MM-dd) | Yes | Offence end date |
| `count` | integer ≥ 0 | Yes | Offence count |
| **`orderIndex`** | **integer ≥ 1** | **No** | **Sequence number of the offence within its prosecution case. Determines display order and labelling.** |
| `plea` | object | Yes | Entered plea, if any |
| `verdict` | object | Yes | Verdict, if any |
| `aagResults` | array | Yes | Judicial results to display on AAAG |
| `custodyTimeLimit` | object | Yes | CTL details, if applicable |
| `reportingRestrictions` | array | Yes | Reporting restrictions on this offence |

### `orderIndex` rules

- MUST match the `order_index` column stored in the view store for this offence.
- MUST be unique within a single prosecution case (no two offences in the same case share the same `orderIndex`).
- Is case-scoped: offence 2 from Case A and offence 2 from Case B are both `orderIndex: 2`. Consumers must not assume global uniqueness across cases.
- MUST NOT be derived from the position of the offence in the API response array. The ordering of offences in the array is not guaranteed to match `orderIndex`.

---

## Acceptance Criteria

### AC1 — Single case application, single offence
**Given** an application linked to one case with one offence (orderIndex = 1)  
**When** the AAAG query is called  
**Then** `linkedCases[0].offences[0].orderIndex` equals `1`

### AC2 — Single case application, multiple offences
**Given** an application linked to one case with two offences (orderIndex = 1 and orderIndex = 2)  
**When** the AAAG query is called  
**Then** `linkedCases[0].offences` contains both `orderIndex: 1` and `orderIndex: 2`  
**And** the `orderIndex` values are not derived from array position

### AC3 — Multi-case application (the CHD-2445 scenario)
**Given** an application linked to Case A (with offences at orderIndex 1 and 2) where offence 1 has been disposed  
**And** the application selects only the active offence from Case A (orderIndex = 2)  
**And** two offences from Case B (orderIndex = 1 and 2)  
**When** the AAAG query is called  
**Then** the offence from Case A has `orderIndex: 2` (not 1)  
**And** the two offences from Case B have `orderIndex: 1` and `orderIndex: 2` respectively  
**And** `orderIndex` values are scoped per case — Case A's `orderIndex: 2` and Case B's `orderIndex: 2` are independent

### AC4 — `orderIndex` is always present
**Given** any application with linked cases  
**When** the AAAG query is called  
**Then** every offence in `linkedCases[].offences[]` has a non-null `orderIndex`

---

## Related Changes

- [CHD-2445: Add orderIndex to AAAG response](../changes/chd-2445-offence-order-index/proposal.md) — the change that implements this spec
- Downstream: `cpp-ui-prosecution-casefile` AAAG template must display `offence.orderIndex` rather than loop position
