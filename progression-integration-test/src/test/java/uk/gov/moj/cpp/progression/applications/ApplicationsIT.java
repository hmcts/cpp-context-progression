package uk.gov.moj.cpp.progression.applications;

import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubEmptyPermissionsQuery;

import uk.gov.moj.cpp.progression.AbstractIT;

import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for application ITs. Resets hearing-type permission WireMock stubs before each test;
 * {@link AbstractIT}'s reset lives in a different package and is not always invoked for subclasses here.
 */
public abstract class ApplicationsIT extends AbstractIT {

    @BeforeEach
    public void resetApplicationPermissionsStub() {
        stubEmptyPermissionsQuery();
    }
}
