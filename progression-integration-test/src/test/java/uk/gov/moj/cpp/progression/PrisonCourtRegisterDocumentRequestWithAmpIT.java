package uk.gov.moj.cpp.progression;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FeatureStubUtil.setFeatureToggle;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider;
import uk.gov.moj.cpp.progression.domain.constant.RegisterType;
import uk.gov.moj.cpp.progression.helper.PrisonCourtRegisterDocumentRequestHelper;
import uk.gov.moj.cpp.progression.stub.AmpPcrEndpointStub;
import uk.gov.moj.cpp.progression.stub.SysDocGeneratorStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Prison Court Register Document Request when AmpSendPcr feature toggle is ENABLED (true).
 * 
 * These tests mirror the tests in PrisonCourtRegisterDocumentRequestIT but with feature guard enabled.
 * They verify that when the feature is enabled:
 *
 * - The existing event is generated (PrisonCourtRegisterGenerated)
 * - The new V2 events (PrisonCourtRegisterGeneratedV2) are generated,
 *   and based on these events, the AMP service is invoked for each generated document.
 * 
 * When the AmpSendPcr feature toggle is removed and becomes the default behavior,
 * these tests will become the standard tests.
 */
public class PrisonCourtRegisterDocumentRequestWithAmpIT extends AbstractIT {
    private ProsecutionCaseUpdateDefendantHelper helper;

    @Test
    public void shouldGeneratePrisonCourtDocumentAsynchronously() throws JSONException {
        setUpAmpStubs();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now(UTC);
        final UUID prisonCourtRegisterStreamId = getPrisonCourtRegisterStreamId(courtCentreId.toString(), hearingDateTime.toLocalDate().toString());

        final PrisonCourtRegisterDocumentRequestHelper prisonCourtRegisterDocumentRequestHelper = new PrisonCourtRegisterDocumentRequestHelper();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final String body = getPayload("progression.prison-court-register-document-request.json")
                .replaceAll("%COURT_CENTRE_ID%", courtCentreId.toString())
                .replaceAll("%HEARING_DATE%", hearingDateTime.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final List<JSONObject> jsonObjects = SysDocGeneratorStub.pollSysDocGenerationRequestsWithOriginatingSourceAndSourceCorrelationId(Matchers.hasSize(1), "PRISON_COURT_REGISTER", prisonCourtRegisterStreamId.toString());
        final JSONObject jsonObject = jsonObjects.get(0);
        final UUID payloadFileServiceId = fromString(jsonObject.getString("payloadFileServiceId"));
        final UUID documentFileServiceId = randomUUID();
        final JSONArray additionalInformationArray = jsonObject.getJSONArray("additionalInformation");
        final String prisonCourtRegisterId = additionalInformationArray.getJSONObject(0).getString("propertyValue");
        prisonCourtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicAvailableEvent(USER_ID_VALUE_AS_ADMIN, prisonCourtRegisterStreamId, payloadFileServiceId, documentFileServiceId, prisonCourtRegisterId);
        prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterIsGenerated(courtCentreId, documentFileServiceId, prisonCourtRegisterId);
        AmpPcrEndpointStub.verifyPostPcrToAmp(1);

        writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final List<JSONObject> jsonObjectsList = SysDocGeneratorStub.pollSysDocGenerationRequestsWithOriginatingSourceAndSourceCorrelationId(Matchers.hasSize(2), "PRISON_COURT_REGISTER", prisonCourtRegisterStreamId.toString());
        final JSONObject jsonObject1 = jsonObjectsList.stream().filter(request -> !request.toString().contains(documentFileServiceId.toString())).findFirst().get();
        final UUID payloadFileServiceId1 = fromString(jsonObject1.getString("payloadFileServiceId"));
        final UUID documentFileServiceId1 = randomUUID();
        final JSONArray additionalInformationArray1 = jsonObject1.getJSONArray("additionalInformation");
        final String prisonCourtRegisterId1 = additionalInformationArray1.getJSONObject(0).getString("propertyValue");
        prisonCourtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicAvailableEvent(USER_ID_VALUE_AS_ADMIN, prisonCourtRegisterStreamId, payloadFileServiceId1, documentFileServiceId1, prisonCourtRegisterId1);
        prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterIsGenerated(courtCentreId, documentFileServiceId1, prisonCourtRegisterId1);
        AmpPcrEndpointStub.verifyPostPcrToAmp(2);
    }

    @Test
    public void shouldAddPrisonCourtDocumentRequestWithApplication() throws IOException, JSONException {
        setUpAmpStubs();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now(UTC);
        final UUID prisonCourtRegisterStreamId = getPrisonCourtRegisterStreamId(courtCentreId.toString(), hearingDateTime.toLocalDate().toString());
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID defendantId = randomUUID();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId.toString(), defendantId.toString());

        final PrisonCourtRegisterDocumentRequestHelper prisonCourtRegisterDocumentRequestHelper = new PrisonCourtRegisterDocumentRequestHelper();

        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = JmsMessageConsumerClientProvider
                .newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        initiateCourtProceedingsForMatchedDefendants(caseId.toString(), defendantId.toString(), defendantId.toString());
        prisonCourtRegisterDocumentRequestHelper.verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        helper.updateDefendantWithCustodyEstablishmentInfo(caseId.toString(), defendantId.toString(), defendantId.toString());
        intiateCourtProceedingForApplication(courtApplicationId.toString(), caseId.toString(), defendantId.toString(), defendantId.toString(), hearingId.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        prisonCourtRegisterDocumentRequestHelper.verifyCourtApplicationCreatedPublicEvent();

        final String body = getPayload("progression.prison-court-register-document-request-with_application.json")
                .replaceAll("%COURT_CENTRE_ID%", courtCentreId.toString())
                .replaceAll("%HEARING_DATE%", hearingDateTime.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%COURT_APPLICATION_ID%", courtApplicationId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId.toString()), hearingId);

        final List<JSONObject> jsonObjects = SysDocGeneratorStub.pollSysDocGenerationRequestsWithOriginatingSourceAndSourceCorrelationId(Matchers.hasSize(1), "PRISON_COURT_REGISTER", prisonCourtRegisterStreamId.toString());
        final JSONObject jsonObject = jsonObjects.get(0);
        final UUID payloadFileServiceId = fromString(jsonObject.getString("payloadFileServiceId"));
        final UUID documentFileServiceId = randomUUID();
        prisonCourtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicAvailableEvent(USER_ID_VALUE_AS_ADMIN, prisonCourtRegisterStreamId, payloadFileServiceId, documentFileServiceId, StringUtils.EMPTY);
        prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterIsGeneratedWithoutPrisonCourtRegisterId(courtCentreId, documentFileServiceId);
        AmpPcrEndpointStub.verifyPostPcrToAmp(1);
    }

    @Test
    public void shouldFailedPrisonCourtDocumentAsynchronously() throws JSONException {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now(UTC);
        final UUID prisonCourtRegisterStreamId = getPrisonCourtRegisterStreamId(courtCentreId.toString(), hearingDateTime.toLocalDate().toString());

        final PrisonCourtRegisterDocumentRequestHelper prisonCourtRegisterDocumentRequestHelper = new PrisonCourtRegisterDocumentRequestHelper();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final String body = getPayload("progression.prison-court-register-document-request.json")
                .replace("%COURT_CENTRE_ID%", courtCentreId.toString())
                .replaceAll("%HEARING_DATE%", hearingDateTime.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);

        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterRequestsExists(courtCentreId);

        final List<JSONObject> jsonObjects = SysDocGeneratorStub.pollSysDocGenerationRequestsWithOriginatingSourceAndSourceCorrelationId(Matchers.hasSize(1), "PRISON_COURT_REGISTER", prisonCourtRegisterStreamId.toString());
        final JSONObject jsonObject = jsonObjects.get(0);
        final UUID payloadFileServiceId = fromString(jsonObject.getString("payloadFileServiceId"));
        final JSONArray additionalInformationArray = jsonObject.getJSONArray("additionalInformation");
        final String prisonCourtRegisterId = additionalInformationArray.getJSONObject(0).getString("propertyValue");
        prisonCourtRegisterDocumentRequestHelper.sendSystemDocGeneratorPublicFailedEvent(USER_ID_VALUE_AS_ADMIN, prisonCourtRegisterStreamId, payloadFileServiceId, prisonCourtRegisterId);
        prisonCourtRegisterDocumentRequestHelper.verifyPrisonCourtRegisterDocumentFailedPrivateTopic(courtCentreId.toString(), payloadFileServiceId.toString(), prisonCourtRegisterId);
    }

    private UUID getPrisonCourtRegisterStreamId(final String courtCentreId, final String hearingDate) {
        return nameUUIDFromBytes((courtCentreId + hearingDate + RegisterType.PRISON_COURT.name()).getBytes());
    }

    private void setUpAmpStubs() {
        AmpPcrEndpointStub.resetRequests();
        setFeatureToggle("AmpSendPcr", true);
        AmpPcrEndpointStub.stubPostPcrToAmp();
    }
}

