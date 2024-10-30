package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_GENERATED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class PrisonCourtRegisterDocumentRequestIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();

    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_RECORDED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient privateEventsConsumer2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_SELECTOR_PRISON_COURT_REGISTER_DOCUMENT_REQUEST_GENERATED).getMessageConsumerClient();
    private String courtCentreId;
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-application-proceedings-initiated").getMessageConsumerClient();
    private ProsecutionCaseUpdateDefendantHelper helper;


    @BeforeEach
    public void setup() {
        stringToJsonObjectConverter = new StringToJsonObjectConverter();
        courtCentreId = randomUUID().toString();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        NotificationServiceStub.setUp();

    }

    @Test
    public void shouldAddPrisonCourtDocumentRequest() throws IOException {

        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final String body = getPayload("progression.prison-court-register-document-request.json")
                .replaceAll("%COURT_CENTRE_ID%", courtCentreId)
                .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));

        JsonPath jsonResponse2 = retrieveMessageAsJsonPath(privateEventsConsumer2);
        assertThat(jsonResponse2.get("courtCentreId"), is(courtCentreId));
        assertThat(jsonResponse2.get("fileId"), is(notNullValue()));
        final String firstFileId = jsonResponse2.get("fileId");


        writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        jsonResponse2 = retrieveMessageAsJsonPath(privateEventsConsumer2);

        assertThat(jsonResponse2.get("fileId"), is(notNullValue()));
        final String secondFileId = jsonResponse2.get("fileId");

        verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId), hearingId, firstFileId);
        verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId), hearingId, secondFileId);
    }

    @Test
    public void shouldAddPrisonCourtDocumentRequestWithApplication() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID defendantId = randomUUID();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId.toString(), defendantId.toString());

        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();

        initiateCourtProceedingsForMatchedDefendants(caseId.toString(), defendantId.toString(), defendantId.toString());
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);

        helper.updateDefendantWithCustodyEstablishmentInfo(caseId.toString(), defendantId.toString(), defendantId.toString());

        intiateCourtProceedingForApplication(courtApplicationId.toString(), caseId.toString(), defendantId.toString(), defendantId.toString(), hearingId.toString(), "applications/progression.initiate-court-proceedings-for-application_for_prison_court_register.json");
        verifyCourtApplicationCreatedPublicEvent();
        final String body = getPayload("progression.prison-court-register-document-request-with_application.json")
                .replaceAll("%COURT_CENTRE_ID%", courtCentreId)
                .replaceAll("%HEARING_DATE%", ZonedDateTime.now(UTC).toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%COURT_APPLICATION_ID%", courtApplicationId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString());

        final Response writeResponse = postCommand(getWriteUrl("/prison-court-register"),
                "application/vnd.progression.add-prison-court-register+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId));
        assertThat(jsonResponse.get("prisonCourtRegister.defendant.prosecutionCasesOrApplications[0].courtApplicationId"), is(courtApplicationId.toString()));

        final JsonPath jsonResponse2 = retrieveMessageAsJsonPath(privateEventsConsumer2);
        assertThat(jsonResponse2.get("courtCentreId"), is(courtCentreId));
        assertThat(jsonResponse2.get("fileId"), is(notNullValue()));
        final String fileId = jsonResponse2.get("fileId");
        verifyPrisonCourtRegisterRequestsExists(UUID.fromString(courtCentreId), hearingId, fileId);
    }


    public void verifyPrisonCourtRegisterRequestsExists(final UUID courtCentreId, final UUID hearingId, final String fileId) {
        final String prisonCourtRegisterDocumentRequestPayload = getPrisonCourtRegisterDocumentRequests(courtCentreId, allOf(
                withJsonPath("$.prisonCourtRegisterDocumentRequests[*].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.prisonCourtRegisterDocumentRequests[*].fileId",  hasItem(fileId))
        ));

        final JsonObject prisonCourtRegisterDocumentRequestJsonObject = stringToJsonObjectConverter.convert(prisonCourtRegisterDocumentRequestPayload);
        final JsonObject prisonCourtRegisterDocumentRequest = prisonCourtRegisterDocumentRequestJsonObject.getJsonArray("prisonCourtRegisterDocumentRequests").getJsonObject(0);
        final String payload = prisonCourtRegisterDocumentRequest.getString("payload");
        final JsonObject payloadJsonObject = stringToJsonObjectConverter.convert(payload);
        assertThat(payloadJsonObject.getString("hearingId"), is(hearingId.toString()));
    }

    private String getPrisonCourtRegisterDocumentRequests(final UUID courtCentreId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(StringUtils.join("/prison-court-register/request/", courtCentreId)),
                "application/vnd.progression.query.prison-court-register-document-by-court-centre+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(60, TimeUnit.SECONDS)
                .until(
                        status().is(javax.ws.rs.core.Response.Status.OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    private void verifyCourtApplicationCreatedPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference, is(notNullValue()));
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated(final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }
}
