package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import org.junit.After;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class ListNewHearingIT extends AbstractIT{

    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String DOCUMENT_TEXT = new StringGenerator().next();
    final MessageConsumer messageConsumerCourtDocumentAddedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.court-document-added");


    @Before
    public void setUp(){
        setupLoggedInUsersPermissionQueryStub();
        stubDocumentCreate(DOCUMENT_TEXT);
    }

    @After
    public void tearDown() throws JMSException {
        messageConsumerCourtDocumentAddedPrivateEvent.close();
    }
    @Test
    public void shouldCreateNewHearing() throws IOException, JMSException {
        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID);

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;
        try (final MessageConsumer messageConsumerListHearingRequested = privateEvents
                .createPrivateConsumer("progression.event.list-hearing-requested")) {

            PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

            final JsonPath message = retrieveMessage(messageConsumerListHearingRequested, isJson(allOf(
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)),
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)),
                    withJsonPath("$.listNewHearing.bookingType", is("Video")),
                    withJsonPath("$.listNewHearing.priority", is("High")),
                    withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                    withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL"))
            )));
            assertNotNull(message);
            hearingId = message.getString("hearingId");

        }

        verifyAddCourtDocument(messageConsumerCourtDocumentAddedPrivateEvent);
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );

    }


    @Test
    public void shouldCreateNewHearing_sendDefendantEmailNotification_NoCPSProsecutorNotification() throws IOException, JMSException {
        final MessageConsumer messageConsumerEmailRequestPrivateEvent = privateEvents.createPrivateConsumer("progression.event.email-requested");
        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID);

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;
        try (final MessageConsumer messageConsumerListHearingRequested = privateEvents
                .createPrivateConsumer("progression.event.list-hearing-requested")) {

            PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

            final JsonPath message = retrieveMessage(messageConsumerListHearingRequested, isJson(allOf(
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)),
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)),
                    withJsonPath("$.listNewHearing.bookingType", is("Video")),
                    withJsonPath("$.listNewHearing.priority", is("High")),
                    withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                    withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL")),
                    withJsonPath("$.sendNotificationToParties", is(true))
                    )));
            assertNotNull(message);
            hearingId = message.getString("hearingId");

        }


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );

        verifyAddCourtDocument(messageConsumerCourtDocumentAddedPrivateEvent);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, CASE_ID);
    }

    @Test
    public void shouldCreateNewHearing_sendDefendantLetterNotification_ProsecutorEmailNotification() throws IOException, JMSException {

        final MessageConsumer messageConsumerEmailRequestPrivateEvent = privateEvents.createPrivateConsumer("progression.event.email-requested");
        final MessageConsumer messageConsumerPrintRequestPrivateEvent = privateEvents.createPrivateConsumer("progression.event.print-requested");
        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps.json", randomUUID());
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation-no-email.json", DEFENDANT_ID);

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;
        try (final MessageConsumer messageConsumerListHearingRequested = privateEvents
                .createPrivateConsumer("progression.event.list-hearing-requested")) {

            PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

            final JsonPath message = retrieveMessage(messageConsumerListHearingRequested, isJson(allOf(
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)),
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)),
                    withJsonPath("$.listNewHearing.bookingType", is("Video")),
                    withJsonPath("$.listNewHearing.priority", is("High")),
                    withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                    withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL"))
            )));
            assertNotNull(message);
            hearingId = message.getString("hearingId");

        }


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );

        verifyAddCourtDocument(messageConsumerCourtDocumentAddedPrivateEvent);

        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, CASE_ID);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerPrintRequestPrivateEvent, CASE_ID);

    }

    private void doVerifyListHearingRequestedPrivateEvent(final MessageConsumer messageConsumerProgressionCommandEmail, final String caseId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProgressionCommandEmail);
        assertThat(message.get(), notNullValue());
        final JsonObject progressionCommandNotificationEvent = message.get();
        assertThat(progressionCommandNotificationEvent.getString("caseId", EMPTY), is(caseId));

    }


    private void verifyAddCourtDocument(MessageConsumer messageConsumerCourtDocumentAddedPrivateEvent) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerCourtDocumentAddedPrivateEvent);
        assertThat(message.get(), notNullValue());
        final JsonObject progressionCourtDocumentAddedEvent = message.get();
        JsonObject courtDocument = progressionCourtDocumentAddedEvent.getJsonObject("courtDocument");
        assertThat(courtDocument.getString("documentTypeDescription"), containsString("Electronic Notifications"));
        assertThat(courtDocument.getString("name"), containsString("NewHearingNotification"));
        assertThat(courtDocument.getBoolean("containsFinancialMeans"), is(false));
        assertThat(courtDocument.getBoolean("sendToCps"), is(false));
    }
}
