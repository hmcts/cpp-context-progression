package uk.gov.moj.cpp.progression.it;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesFeatureDisabled;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub;
import uk.gov.moj.cpp.progression.helper.StubUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class AddDefendantAdditionalInfoIT {

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
    public void shouldAddAdditionalInfoForDefendant() throws Exception {
        addDefendant(caseId,firstDefendantId);
        addDefendant(caseId,secondDefendantId);
        Response writeResponse ;

        final String queryResponse =
                pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                        "application/vnd.progression.query.defendant+json");

        JsonObject defendantsJsonObject = getJsonObject(queryResponse);

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                equalTo(Boolean.FALSE));

        writeResponse = postAddDefendantAdditionalInfoCommand(firstDefendantId);
        assertThatRequestIsAccepted(writeResponse);

        final String queryDefendantsResponse = pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");


        defendantsJsonObject = getJsonObject(queryDefendantsResponse);
        assertThat(defendantsJsonObject.getString("status"), equalTo("INCOMPLETE"));


        final String queryProgressionDefendantsResponse =
                pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                        "application/vnd.progression.query.defendant+json");

        defendantsJsonObject = getJsonObject(queryProgressionDefendantsResponse);

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("probation").getJsonObject("preSentenceReport")
                .getBoolean("psrIsRequested"), equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("probation").getJsonObject("preSentenceReport")
                .getString("provideGuidance"), equalTo("guidance"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("probation").getJsonObject("preSentenceReport")
                .getBoolean("drugAssessment"), equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                        .getJsonObject("probation").getBoolean("dangerousnessAssessment"),
                equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("defence").getJsonObject("statementOfMeans")
                .getBoolean("isStatementOfMeans"), equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("defence").getJsonObject("statementOfMeans")
                .getString("details"), equalTo("meansDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("defence").getJsonObject("medicalDocumentation")
                .getBoolean("isMedicalDocumentation"), equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("defence").getJsonObject("medicalDocumentation")
                .getString("details"), equalTo("medicalDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                        .getJsonObject("defence").getString("otherDetails"),
                equalTo("otherDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("prosecution").getJsonObject("ancillaryOrders")
                .getBoolean("isAncillaryOrders"), equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getJsonObject("prosecution").getJsonObject("ancillaryOrders")
                .getString("details"), equalTo("ancillaryOrdersDetails"));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                        .getJsonObject("prosecution").getString("otherDetails"),
                equalTo("otherDetails"));

        writeResponse = postAddDefendantAdditionalInfoCommand(secondDefendantId);
        assertThatRequestIsAccepted(writeResponse);

        final String queryProgressionSentenceResponse = pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(queryProgressionSentenceResponse);
        assertThat(defendantsJsonObject.getString("status"),
                equalTo("PENDING_FOR_SENTENCING_HEARING"));
    }

    @Test
    public void shouldNotAddAdditionalInfoForDefendant_CapabilityDisabled() throws Exception {
        givenAddDefendantAdditionalInformationCapabiltyDisabled();
        addDefendant(caseId,firstDefendantId);
        final Response writeAdditionalInfoResponse = postAddDefendantAdditionalInfoCommand(firstDefendantId);
        assertThatResponseIndicatesFeatureDisabled(writeAdditionalInfoResponse);
    }

    @Test
    public void shouldSetNoMoreInformationRequired() throws Exception {
        addDefendant(caseId,firstDefendantId);
        addDefendant(caseId,secondDefendantId);

        final String response =
                pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                        "application/vnd.progression.query.defendant+json");

        JsonObject defendantsJsonObject = getJsonObject(response);

        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                equalTo(Boolean.FALSE));

        Response writeResponse = postNoMoreInformationRequiredCommand(firstDefendantId);
        assertThatRequestIsAccepted(writeResponse);

        final String queryResponse = pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(queryResponse);
        assertThat(defendantsJsonObject.getString("status"), equalTo("INCOMPLETE"));

        final String queryDefendantResponse =
                pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                        "application/vnd.progression.query.defendant+json");


        defendantsJsonObject = getJsonObject(queryDefendantResponse);
        assertThat(defendantsJsonObject.getBoolean("sentenceHearingReviewDecision"),
                equalTo(Boolean.TRUE));
        assertThat(defendantsJsonObject.getJsonObject("additionalInformation")
                .getBoolean("noMoreInformationRequired"), equalTo(Boolean.TRUE));

        LocalDateTime reviewDecisionDateTime = LocalDateTime.parse(
                defendantsJsonObject.getString("sentenceHearingReviewDecisionDateTime"));
        assertThat(reviewDecisionDateTime, is(notNullValue()));

        writeResponse = postNoMoreInformationRequiredCommand(secondDefendantId);
        assertThatRequestIsAccepted(writeResponse);

        final String querySentenceResponse = pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");

        defendantsJsonObject = getJsonObject(querySentenceResponse);
        assertThat(defendantsJsonObject.getString("status"),
                equalTo("READY_FOR_SENTENCING_HEARING"));
    }

    @Test
    public void shouldNotSetNoMoreInformationRequired_CapabilityDisabled() throws Exception {
        givenNoMoreInformationRequiredCapabiltyDisabled();
        addDefendant(caseId,firstDefendantId);
        final Response writeAdditionalInfoResponse = postNoMoreInformationRequiredCommand(firstDefendantId);
        assertThatResponseIndicatesFeatureDisabled(writeAdditionalInfoResponse);
    }

    private Response postNoMoreInformationRequiredCommand(String defendantId) throws IOException {
        return postCommand(getCommandUri("/cases/" + caseId + "/defendants/" + defendantId),
                "application/vnd.progression.command.no-more-information-required+json",
                getJsonBody("progression.command.no-more-information-required.json"));
    }

    private Response postAddDefendantAdditionalInfoCommand(String defendantId) throws IOException {
        return postCommand(getCommandUri("/cases/" + caseId + "/defendants/" + defendantId),
                "application/vnd.progression.command.add-defendant-additional-information+json",
                getJsonBody("progression.command.add-defendant-additional-information.json"));
    }

    private String getJsonBody(String path) {
        return StubUtil.getJsonBodyStr(path, caseId, firstDefendantId, secondDefendantId, caseProgressionId);
    }

    private void givenAddDefendantAdditionalInformationCapabiltyDisabled() {
        stubSetStatusForCapability("progression.command.add-defendant-additional-information", false);
    }

    private void givenNoMoreInformationRequiredCapabiltyDisabled() {
        stubSetStatusForCapability("progression.command.no-more-information-required", false);
    }
}
