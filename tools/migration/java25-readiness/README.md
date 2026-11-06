# Java 25 prerequisite probe — roadmap Step 10

This independent module is outside the Java 17 application reactor. Its dedicated
PR stage uses the existing ubuntu-j25 agent demand. Normal application CI stays
on Java 17. No deployment, image build, database migration or business change is
performed. The excluded upgrade branch is not an implementation source.

Released source selects service-parent25.104.6, platform-libraries25.104.6,
common parent/BOM25.104.5, framework25.104.6 and libraries25.104.5. Maven on the
approved CI agent must establish publication and actual resolution.

The three smoke tests check Java25, Jakarta API/framework loading, explicit
managed Lombok processing and the intended Hibernate7 provider family. The
inspected BOM currently manages Hibernate6.6.1: that mismatch must fail readiness
until provider/runtime alignment is qualified. No context override conceals it.

Both existing MI classifier coordinates are retained for resolution/inspection.
RELEASE is used only to inspect what is published; it cannot pass the pin gate.
Supply a qualified explicit release in both POM dependencies and --mi-version.
The inspector records archive hashes, classfile ranges and legacy JavaEE refs.
It preserves JavaSE javax namespaces. It does not execute MI contracts or prove
compatibility from a version name. Main's actual MI test extraction stays intact.

The stage publishes synthetic smoke-test results and bounded inspection metadata,
including on failure. Missing dependencies, tests or classifiers cannot pass.
No Maven settings, environment dumps, class payloads or test fixtures are archived.

Run parser checks locally with:

```sh
python3 -B -m unittest discover -s tools/migration/java25-readiness -p 'test_*.py'
```

Steps05–09 retain the agreed Java17 checkpoint. Full target application Step11
depends on that checkpoint and this qualification, then coordinated Jakarta,
generator, persistence and test changes. Steps12–14 also require matched runtime,
business evidence and separate deployment authority. Creating this probe does
not complete those steps or upgrade the Progression application.
