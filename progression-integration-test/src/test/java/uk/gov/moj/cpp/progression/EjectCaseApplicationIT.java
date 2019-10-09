package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.ejectCaseApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationWithMatchingApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.nio.charset.Charset;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class EjectCaseApplicationIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String CASE_OR_APPLICATION_EJECTED
            = "public.progression.events.case-or-application-ejected";

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final MessageConsumer consumerForCaseOrAPplicationEjected = publicEvents.createConsumer(CASE_OR_APPLICATION_EJECTED);
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final String REMOVAL_REASON = "Legal";
    private static final String STATUS_EJECTED = "EJECTED";
    private static final String STATUS_DRAFT = "DRAFT";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String userId;

    @Before
    public void setUp() {
        createMockEndpoints();
        HearingStub.stubInitiateHearing();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        hearingId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }
    @Test
    public void shouldEjectStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String applicationId = randomUUID().toString();
        addStandaloneCourtApplication(applicationId, randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-applications-only.json",
                        caseId, hearingId, randomUUID().toString(), courtCentreId, applicationId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForApplicationStatus(applicationId, "LISTED");
        ejectCaseApplication(applicationId, caseId, REMOVAL_REASON, "eject/progression.eject-application.json");

        String applicationResponse = getApplicationWithMatchingApplicationStatus(applicationId, STATUS_EJECTED);
        JsonObject applicationJson = getJsonObject(applicationResponse);
        JsonObject courtApplication = applicationJson.getJsonObject("courtApplication");
        assertThat(courtApplication.getString("id"), equalTo(applicationId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo(STATUS_EJECTED));
        assertThat(courtApplication.getString("removalReason"), equalTo(REMOVAL_REASON));
        verifyInMessagingQueueForCaseOrApplicationEjected();

        final String hearingIdQueryResult = pollForResponse("/hearingSearch/"+ hearingId, PROGRESSION_QUERY_HEARING_JSON);
        final JsonObject hearingJsonObject = getJsonObject(hearingIdQueryResult);
        JsonObject courtApplicationInHeaering = hearingJsonObject.getJsonObject("hearing").getJsonArray("courtApplications").getJsonObject(0);
        assertThat(courtApplicationInHeaering.getString("applicationStatus"), equalTo(STATUS_EJECTED));
    }

    @Test
    public void shouldEjectStandaloneCourtApplicationWithoutHearingIdAndGetConfirmation() throws Exception {

        String applicationId = randomUUID().toString();
        addStandaloneCourtApplication(applicationId, randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        pollForApplicationStatus(applicationId, STATUS_DRAFT);
        ejectCaseApplication(applicationId, caseId, REMOVAL_REASON, "eject/progression.eject-application.json");

        String applicationResponse = getApplicationFor(applicationId);
        JsonObject applicationJson = getJsonObject(applicationResponse);
        JsonObject courtApplication = applicationJson.getJsonObject("courtApplication");
        assertThat(courtApplication.getString("id"), equalTo(applicationId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo(STATUS_EJECTED));
        assertThat(courtApplication.getString("removalReason"), equalTo(REMOVAL_REASON));
        verifyInMessagingQueueForCaseOrApplicationEjected();
    }

    @Test
    public void shouldCreateCourtApplicationLinkedWithCaseAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        addCourtApplication(caseId, firstApplicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");
        //assert first application
        String applicationResponse = getApplicationFor(firstApplicationId);
        assertSingleApplication(applicationResponse, STATUS_DRAFT);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId, randomUUID().toString() ), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        // Creating second application for the case
        String secondApplicationId = randomUUID().toString();
        addCourtApplication(caseId, secondApplicationId, "progression.command.create-court-application.json");
        verifyInMessagingQueueForCourtApplicationCreated(reference + "-2");
        //assert second application
        applicationResponse = getApplicationFor(secondApplicationId);
        assertSingleApplication(applicationResponse, STATUS_DRAFT);

        //assert linked applications
        String caseResponse = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutionCaseJson = getJsonObject(caseResponse);
        assertLinkedApplications(prosecutionCaseJson, STATUS_DRAFT);

        // Eject case
        ejectCaseApplication(randomUUID().toString(), caseId, REMOVAL_REASON, "eject/progression.eject-case.json");
        caseResponse = getProsecutioncasesProgressionFor(caseId);
        prosecutionCaseJson = getJsonObject(caseResponse);
        // Assert case status ejected
        assertThat(prosecutionCaseJson.getJsonObject("prosecutionCase").getString("caseStatus"), equalTo(STATUS_EJECTED));
        assertThat(prosecutionCaseJson.getJsonObject("prosecutionCase").getString("removalReason"), equalTo(REMOVAL_REASON));
        assertLinkedApplications(prosecutionCaseJson, STATUS_EJECTED);

        // assert applications ejected
        applicationResponse = getApplicationFor(firstApplicationId);
        assertSingleApplication(applicationResponse, STATUS_EJECTED);

        applicationResponse = getApplicationFor(secondApplicationId);
        assertSingleApplication(applicationResponse, STATUS_EJECTED);
        verifyInMessagingQueueForCaseOrApplicationEjected();

        final String hearingIdQueryResult = pollForResponse("/hearingSearch/"+ hearingId, PROGRESSION_QUERY_HEARING_JSON);
        final JsonObject hearingJsonObject = getJsonObject(hearingIdQueryResult);
        final JsonObject prosecutionCaseInHearing = hearingJsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0);
        assertThat(prosecutionCaseInHearing.getString("caseStatus"), equalTo(STATUS_EJECTED));

    }

    private void assertSingleApplication(String applicationResponse, final String status) {
        JsonObject applicationJson = getJsonObject(applicationResponse);
        JsonObject courtApplication = applicationJson.getJsonObject("courtApplication");

        assertThat(courtApplication.getString("applicationStatus"), equalTo(status));
        if (STATUS_EJECTED.equals(status)) {
            assertThat(courtApplication.getString("removalReason"), equalTo(REMOVAL_REASON));
        }
    }

    private void assertLinkedApplications(JsonObject prosecutionCaseJson, final String status) {
        JsonArray summaries = prosecutionCaseJson.getJsonArray("linkedApplicationsSummary");

        assertThat(summaries.size(), equalTo(2));

        JsonObject summary = summaries.getJsonObject(0);

        assertThat(summary.getString("applicationStatus"), equalTo(status));
        if (STATUS_EJECTED.equals(status)) {
            assertThat(summary.getString("removalReason"), equalTo(REMOVAL_REASON));
        }

        summary = summaries.getJsonObject(1);

        assertThat(summary.getString("applicationStatus"), equalTo(status));
        if (STATUS_EJECTED.equals(status)) {
            assertThat(summary.getString("removalReason"), equalTo(REMOVAL_REASON));
        }
    }


    private static void verifyInMessagingQueueForCourtApplicationCreated(String arn) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertThat(arnResponse, equalTo(arn));
    }

    private static void verifyInMessagingQueueForCaseOrApplicationEjected() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCaseOrAPplicationEjected);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertTrue(arnResponse.length() == 10);
    }

    public JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                           final String defendantId, final String courtCentreId, String applicationId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }
}

