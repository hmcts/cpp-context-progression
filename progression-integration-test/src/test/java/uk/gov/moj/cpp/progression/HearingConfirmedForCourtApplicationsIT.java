package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.verifyPostInitiateCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.test.TestUtilities.print;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("squid:S1607")
public class HearingConfirmedForCourtApplicationsIT extends AbstractIT {
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private MessageProducer messageProducerClientPublic;
    private MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged;
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK = "progression.event.hearing-application-link-created";
    private static final String PROGRESSION_COMMAND_SEND_NOTIFICATION_FOR_AUTO_APPLICATION_INITIATED = "progression.event.send-notification-for-auto-application-initiated";
    private  MessageConsumer messageConsumerLink;
    private  MessageConsumer messageConsumerAutoNotification;
    private static final String MAGISTRATES_JURISDICTION_TYPE = "MAGISTRATES";
    private  MessageConsumer messageConsumerListingNumberUpdated;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String applicationId;

    @After
    public void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
        messageConsumerAutoNotification.close();
    }

    @Before
    public void setUp() {
        DocumentGeneratorStub.stubDocumentCreate(STRING.next());
        HearingStub.stubInitiateHearing();
        IdMapperStub.setUp();
        userId = randomUUID().toString();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");

        messageProducerClientPublic = publicEvents.createPublicProducer();
        messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2");

        messageConsumerLink = privateEvents.createPrivateConsumer(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK);
        messageConsumerAutoNotification = privateEvents.createPrivateConsumer(PROGRESSION_COMMAND_SEND_NOTIFICATION_FOR_AUTO_APPLICATION_INITIATED);

        messageConsumerListingNumberUpdated = privateEvents.createPrivateConsumer("progression.event.listing-number-updated");

    }

    @Test
    public void shouldUpdateCaseLinkedApplicationStatus() throws Exception {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        applicationId = UUID.randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");
        pollForApplication(applicationId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-application-with-linked-case.json",
                        caseId, hearingId, defendantId, courtCentreId, courtCentreName), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());
        pollForApplicationStatus(applicationId, "LISTED");
        pollForApplicationAtAGlance("LISTED");
        verifyPostInitiateCourtHearing(hearingId);
        verifyInMessagingQueue();
        verifyInMessagingQueueForAutoNotification();

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
        return stringToJsonObjectConverter.convert(strPayload);
    }


    private void pollForApplicationAtAGlance(final String status) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_JSON)
                .withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                                withJsonPath("$.hearingsAtAGlance.courtApplications.[*].courtApplicationCases[*].prosecutionCaseId", hasItem(equalTo(caseId))),
                                withJsonPath("$.hearingsAtAGlance.courtApplications.[*].applicationStatus", hasItem(equalTo(status))),
                                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(courtCentreId))),
                                withJsonPath("$.hearingsAtAGlance.latestHearingJurisdictionType", equalTo(MAGISTRATES_JURISDICTION_TYPE))
                        )));
    }

    private void verifyInMessagingQueue() {
        Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(QueueUtil.retrieveMessage(messageConsumerLink), notNullValue()));
    }

    private void verifyInMessagingQueueForAutoNotification() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerAutoNotification);
        assertTrue(message.isPresent());
    }

}
