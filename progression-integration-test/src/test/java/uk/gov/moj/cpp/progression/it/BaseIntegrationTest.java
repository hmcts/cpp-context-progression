package uk.gov.moj.cpp.progression.it;

import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;

import org.junit.Before;

public abstract class BaseIntegrationTest {

    @Before
    public void setup() {
        stubEnableAllCapabilities();
    }
}
