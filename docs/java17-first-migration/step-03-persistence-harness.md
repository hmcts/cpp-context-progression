# Step 03 Independent RelatedReference JPA fixture

This branch adds test support only. Existing CdiTestRunner/OpenEJB tests,
production persistence.xml, Maven dependencies and runtime settings remain intact.

RelatedReferenceJpaTestContext bootstraps the existing classpath's Hibernate
provider with standard Javax persistence-unit SPI metadata. It explicitly lists
one real entity and uses a unique H2 in-memory database for each fixture. This
avoids adding a test META-INF/persistence.xml that shadows the application unit.
No Hibernate version is guessed or pinned.

The tests exercise actual resource-local commit and native SQL read, rollback
after flush, independent fixture databases and disposal of EntityManagers.
These are resource-local H2 tests: they do not demonstrate PostgreSQL semantics,
WildFly/CDI integration, JTA joining, container transaction ownership or production
behaviour. The existing container tests remain for comparison.

## Acceptance boundary

This is a narrow test-support slice of Step 03. Compile and execute it on the
resolved Java 17 baseline before acceptance. The roadmap's independent real
PostgreSQL and container/JTA gates remain open until their execution evidence is
retained. An H2 helper test pass cannot close those gates.
