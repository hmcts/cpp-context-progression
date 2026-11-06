# Step 01 Java 17 baseline

This branch prepares baseline evidence collection without changing production code,
Maven dependencies, test selection, runtime configuration or business rules.

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

## Current boundary

This branch supplies the collector and verification entry point. A local build
cannot yet resolve the private parent using the available Maven configuration;
the existing CI/build environment is the selected execution route. No baseline
application test pass, integration pass, merge or deployment is claimed here.
Later branches remain dependent on executed evidence, not branch creation.
