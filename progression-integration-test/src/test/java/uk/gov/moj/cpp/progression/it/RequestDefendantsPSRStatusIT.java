package uk.gov.moj.cpp.progression.it;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseProgression;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.givenCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesFeatureDisabled;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesSuccess;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.moj.cpp.progression.helper.StubUtil;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class RequestDefendantsPSRStatusIT {

    private String caseId;
    private String caseProgressionId;
    private String firstDefendantId;
    private String secondDefendantId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = caseId;
        firstDefendantId = UUID.randomUUID().toString();
        secondDefendantId = UUID.randomUUID().toString();
        createMockEndpoints();

    }

    @Test
    public void shouldRequestPSRForDefendant() throws Exception {
        addDefendant(caseId,firstDefendantId);
        addDefendant(caseId,secondDefendantId);

        pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                "application/vnd.progression.query.defendant+json");

        Response writeResponse = postCommand(getCommandUri("/cases/" + caseId + "/defendants/requestpsr"),
                "application/vnd.progression.command.request-psr-for-defendants+json",
                StubUtil.getJsonBodyStr(
                        "progression.command.request-psr-for-defendants.json",
                        caseId, firstDefendantId, secondDefendantId, caseProgressionId));

        assertThatRequestIsAccepted(writeResponse);

        final String defendantsResponse =
                pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                        "application/vnd.progression.query.defendant+json");

        JsonObject defendantsJsonObject = getJsonObject(defendantsResponse);

        assertThatPSRNotRequestedForDefendant(defendantsJsonObject);

        Response queryResponse = getCaseProgression(
                getQueryUri("/cases/" + caseId + "/defendants/" + secondDefendantId),
                "application/vnd.progression.query.defendant+json");
        assertThatResponseIndicatesSuccess(queryResponse);

        defendantsJsonObject = getJsonObject(queryResponse.getBody().asString());

        assertThatPSRRequestedForDefendant(defendantsJsonObject);
            }

    @Test
    public void shouldBeUnableToRequestPSRForDefendant_CapabilityDisabled() throws Exception {
        givenRequestPSRForDefendantCapabilityDisabled();
        addDefendant(caseId,firstDefendantId);
        addDefendant(caseId,secondDefendantId);

        Response writeResponse = postCommand(getCommandUri("/cases/" + caseId + "/defendants/requestpsr"),
                "application/vnd.progression.command.request-psr-for-defendants+json",
                StubUtil.getJsonBodyStr(
                        "progression.command.request-psr-for-defendants.json",
                        caseId, firstDefendantId, secondDefendantId, caseProgressionId));

        assertThatResponseIndicatesFeatureDisabled(writeResponse);
    }

    private void givenRequestPSRForDefendantCapabilityDisabled() {
        stubSetStatusForCapability("progression.command.request-psr-for-defendants", false);
    }

    private void assertThatPSRRequestedForDefendant(JsonObject defendantsJsonObject) {
        assertThatPSRRequestedIs(Boolean.TRUE, defendantsJsonObject);
    }

    private void assertThatPSRNotRequestedForDefendant(JsonObject defendantsJsonObject) {
        assertThatPSRRequestedIs(Boolean.FALSE, defendantsJsonObject);
    }

    private void assertThatPSRRequestedIs(Boolean isRequested, JsonObject defendantsJsonObject) {
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("probation").getJsonObject("preSentenceReport")
                .getBoolean("psrIsRequested"), equalTo(isRequested));
    }
}
