package uk.gov.moj.cpp.progression.it;

import org.junit.Before;

import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubEnableAllCapabilities;

public abstract class BaseIntegrationTest {

    @Before
    public void setup() {
        stubEnableAllCapabilities();
    }
}
