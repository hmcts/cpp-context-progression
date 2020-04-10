package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.invokeDisassociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationDisassociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForDefenceUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPSNotificationIT extends AbstractIT {
    private static final String PUBLIC_DEFENCE_RECORD_INSTRUCTED = "public.defence.event.record-instruction-details";
    private static final String PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE = "public.defence.event.record-instruction-details.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed-cps-notification.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(CPSNotificationIT.class.getCanonicalName());


    private static final MessageProducer PUBLIC_MESSAGE_CONSUMER = publicEvents.createProducer();
    private static final MessageConsumer NOTIFICATION_EMAIL_REQUESTED = privateEvents.createConsumer("progression.event.email-requested");
    private static final MessageConsumer NOTIFICATION_REQUEST_ACCEPTED = privateEvents.createConsumer("progression.event.notification-request-accepted");
    private static final String ORGANISATION_ID = "f8254db1-1683-483e-afb3-b87fde5a0a26";
    private static final String ORGANISATION_NAME = "Smith Associates Ltd.";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;

    @AfterClass
    public static void tearDown() throws JMSException {
        NOTIFICATION_REQUEST_ACCEPTED.close();
        PUBLIC_MESSAGE_CONSUMER.close();
        NOTIFICATION_EMAIL_REQUESTED.close();
    }

    @Before
    public void setUp() {
        IdMapperStub.setUp();
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        stubGetOrganisationDetails(ORGANISATION_ID, ORGANISATION_NAME);
    }

    @Test
    public void shouldNotifyCPS() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        sendMessage(PUBLIC_MESSAGE_CONSUMER,
                PUBLIC_LISTING_HEARING_CONFIRMED, getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                        caseId, hearingId, defendantId, courtCentreId, courtCentreName), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        verifyHearingInitialised(caseId, hearingId);

        // Instruct
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_DEFENCE_RECORD_INSTRUCTED)
                .withUserId(userId)
                .build();

        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        sendMessage(PUBLIC_MESSAGE_CONSUMER,
                PUBLIC_DEFENCE_RECORD_INSTRUCTED, recordInstructedPublicEvent, metadata);

        // notify by email
        verifyEvent(NOTIFICATION_EMAIL_REQUESTED);
        verifyEvent(NOTIFICATION_REQUEST_ACCEPTED);

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, ORGANISATION_ID, ORGANISATION_NAME);
        stubGetOrganisationDetails(ORGANISATION_ID, ORGANISATION_NAME);

        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {

            // Associate
            associateOrganisation(defendantId, userId);
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, ORGANISATION_ID);
            verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                    ORGANISATION_ID,
                    userId);

            // Disassociate
            final Response response = invokeDisassociateOrganisation(defendantId, userId, ORGANISATION_ID, caseId);
            assertThat(response.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
            //Then
            helper.verifyDefenceOrganisationDisassociatedEventGenerated(defendantId, ORGANISATION_ID);
            verifyDefenceOrganisationDisassociatedDataPersisted(defendantId, ORGANISATION_ID, userId);
        }

        // notify by email
        verifyEvent(NOTIFICATION_EMAIL_REQUESTED);
        verifyEvent(NOTIFICATION_REQUEST_ACCEPTED);
    }

    private JsonObject getInstructedJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName);
        LOGGER.info("Payload: " + strPayload);
        LOGGER.info("COURT_CENTRE_ID==" + courtCentreId);
        LOGGER.info("COURT_CENTRE_NAME==" + courtCentreName);
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

    private void verifyEvent(MessageConsumer event) {
        final JsonPath jsonResponse = retrieveMessage(event);
        assertThat(jsonResponse.get("caseId"), CoreMatchers.is(caseId));
    }

    private static void verifyHearingInitialised(final String caseId, final String hearingId) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearingsAtAGlance.hearings[0].id", CoreMatchers.equalTo(hearingId)),
                                withJsonPath("$.hearingsAtAGlance.hearings[0].hearingListingStatus", CoreMatchers.equalTo("HEARING_INITIALISED"))
                        )));
    }
}

