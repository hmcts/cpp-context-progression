# Step 01 Java 17 baseline

This branch prepares baseline evidence collection and a narrowly qualified
Listing command RAML refresh. Java, runtime/provider, schemas, test selection and
business rules remain on the current stack.

Source baseline: `ceebd3c81e899e1dfea4814de9016a49512d806a` on `main`.
The planning branch is separate. The later Java upgrade branch is not used.

## Run in the existing verification environment

Use a draft PR from `migration/progression-01-j17-baseline` to `main`.
The repository YAML routes `Build.Reason=PullRequest` to `context-verify.yaml`.
The source branch does not match the `main` or `team/*` push-trigger patterns.
The verification template's release-versioning condition does not match this
source/target pair. Do not change the branch to a `team/*` or `release/*` route.

The reviewed template main revision was
`c0a93fdebdd2a2f79ab88e57f5d20ded0a74eeae`; record the actual resolved revision
from the run because the repository currently references the moving `main` ref.
Verification executes Maven `verify` and Sonar analysis. It does not establish
full deployed integration-journey results. Do not execute `runIntegrationTests.sh`
as an evidence collector: that script also starts/deploys services and runs
Liquibase migrations.

After a clean verification build, in that same checkout/job environment:

```sh
python3 tools/migration/collect_baseline.py \
  --output target/migration-baseline.json --require-passing-reports
```

The collector reads source metadata and existing Surefire/Failsafe report counts.
It does not build or deploy. It never reads Maven credentials or collects test
payloads, exception messages or environment dumps. Missing reports cannot satisfy
`--require-passing-reports`. Counts alone do not prove report freshness: retain
the clean-build logs and revision alongside the summary. Do not commit the output.

Run the collector's parser checks with:

```sh
python3 -m unittest discover -s tools/migration -p 'test_*.py'
```

## Evidence required before accepting this step

- Actual source/merge SHA, agent JDK/Maven and resolved template revisions.
- Effective dependency/runtime manifest, with credentials excluded.
- Discovered/executed/skipped/failing test totals and linked reports.
- Exact failures classified as baseline behaviour, dependency/environment issue
  or migration regression; no missing tests silently treated as passes.
- Separate authorised integration evidence for the required business journeys.

## Baseline interface enforcement failure and bounded fix

The first Maven attempt in run 949881 failed `enforce-moj-latest-interfaces` in
progression-event-processor: listing-command-api 17.104.10 was behind released
17.104.16. This occurred on the initial branch containing no Java or POM changes.
The existing template then retried the whole Maven command automatically.

The fix introduces listing.command.api.version only for the generator's existing
listing-command-api RAML classifier. Its transitive exclusions remain intact.
listing.version and every Listing query consumer stay at 17.104.10. No Listing
server Java or business implementation is imported.

Released source compared: v17.104.10 at
c71d27170d246cbe9f2cac02513672f0aec5aa4b and v17.104.16 at
c221cdfb6108925bdc032ecd199ddadb00896a00 in cpp-context-listing.
The root listing-command-api.raml, its media/action mappings, the three commands
used by Progression (list-court-hearing, list-unscheduled-court-hearing and
delete-hearing), and their examples are byte-identical between those releases.
The three other changed command schemas add only optional
judiciaryAssignmentSource with MANUAL/AUTO values; required fields are unchanged.
Source qualification supports this bounded build fix; CI must still verify
generation, compilation and tests. Do not suppress the interface-enforcement rule.

## Current boundary

This branch supplies the collector and verification entry point. A local build
cannot yet resolve the private parent using the available Maven configuration;
the existing CI/build environment is the selected execution route. No baseline
application test pass, integration pass, merge or deployment is claimed here.
Later branches remain dependent on executed evidence, not branch creation.

## Verification cache failure

Run 949979 passed Maven verification and Sonar reporting, then failed in
Post-job: Restore/save Maven cache (.m2). The archive step reported a tar short
write (4096 of 10240 bytes) and exit code 2. That identifies the failed cache
operation; it does not establish the underlying storage cause.

The PR verification route now passes the template's supported
enableMavenCache=false parameter for this repository. This avoids the optional
cache restore/archive and uses Maven's normal local repository. Maven verify,
Sonar analysis/reporting, test selection and normal enforcement remain enabled.
The IndividualCI validation/deployment route is unchanged. Re-enable caching only
after its archive/storage behaviour is qualified separately.

Run 949975 separately passed Maven verification but its own Sonar compute-engine
task 8e5347a4-397d-4099-8db7-3a5f2d6ee1d2 failed. Scanner and reporting task IDs
match. The detailed server error remains unavailable because the desktop cannot
validate the Sonar endpoint's certificate. The cache opt-out does not resolve or
waive that analysis failure; fresh CI results must be retained with this history.
