package uk.gov.moj.cpp.progression.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesFeatureDisabled;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class AddCaseToCrownCourtIT {

    private String caseId;
    private String caseProgressionId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = UUID.randomUUID().toString();
        createMockEndpoints(caseId);
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        // given
        addCaseToCrownCourt(caseId, caseProgressionId);

        // when
        final String response = getCaseProgressionFor(caseId);

        // then
        final JsonObject defendantsJsonObject = getJsonObject(response);
        assertThat(defendantsJsonObject.getString("courtCentreId"), equalTo("courtCentreId"));
    }

    @Test
    public void shouldNotAddCaseToCrownCourt_CapabilityDisabled() throws Exception {
        givenAddCaseToCrownCourtFeatureDisabled();

        final Response writeResponse = addCaseToCrownCourt(caseId, caseProgressionId);

        assertThatResponseIndicatesFeatureDisabled(writeResponse);
    }

    private void givenAddCaseToCrownCourtFeatureDisabled() {
        stubSetStatusForCapability("progression.command.add-case-to-crown-court", false);
    }
}
