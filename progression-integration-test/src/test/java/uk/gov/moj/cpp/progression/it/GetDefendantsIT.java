package uk.gov.moj.cpp.progression.it;

import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.givenCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesFeatureDisabled;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

import java.io.IOException;
import java.util.UUID;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class GetDefendantsIT {

    private String caseId;
    private String caseProgressionId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
        createMockEndpoints();
    }

    @Test
    public void shouldNotReturnCaseDefendants_CapabilityDisabled() throws Exception {
        givenQueryDefendantsCapabilityDisabled();
        givenCaseAddedToCrownCourt(caseId, caseProgressionId);

        Response queryResponse = getDefendants(caseId);

        assertThatResponseIndicatesFeatureDisabled(queryResponse);
    }


    private void givenQueryDefendantsCapabilityDisabled() {
        stubSetStatusForCapability("progression.query.defendants", false);
    }
}
