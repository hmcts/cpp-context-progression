# Step 02 Initial repository characterization

This stacked branch adds assertions against the existing implementation. It
changes no production code, dependencies, descriptors or selected test runners.

- MatchDefendantCaseHearingRepository must reject two physical rows matching its
  optional single-result lookup. No-match and one-match coverage already exists.
  The new fixture uses its own IDs and removes its ambiguous tuple explicitly.
- RelatedReferenceRepository is the proposed narrow pilot. Its current save/read,
  missing ID, case filtering, no-match, same-ID update and deletion outcomes are
  captured before implementation changes.

The existing JUnit 4/CdiTestRunner harness remains. No new persist-versus-merge,
object-identity, transaction ownership or timing rule is assumed here. Those
require actual released dependency and container evidence before Step 04.

## Acceptance boundary

This is an initial slice of Step 02, not a completed characterization of every
repository and business journey. Execute against the same Java 17 source/runtime
baseline as Step 01. Preserve meaningful exclusions and distinguish physical
row identity from entity equality. Record run results before acceptance.
The branch must remain unmerged while baseline or relevant tests are unresolved.
