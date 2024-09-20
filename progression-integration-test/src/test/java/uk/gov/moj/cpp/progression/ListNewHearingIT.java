package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;

import java.io.IOException;
import java.util.Optional;

import javax.jms.JMSException;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListNewHearingIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String DOCUMENT_TEXT = new StringGenerator().next();

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();
        stubDocumentCreate(DOCUMENT_TEXT);
    }

    @Test
    public void shouldCreateNewHearing() throws IOException, JMSException, JSONException {
        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID);

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;
        final JmsMessageConsumerClient messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.list-hearing-requested").getMessageConsumerClient();

        PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListHearingRequested, isJson(allOf(
                withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)),
                withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)),
                withJsonPath("$.listNewHearing.bookingType", is("Video")),
                withJsonPath("$.listNewHearing.priority", is("High")),
                withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL"))
        )));
        assertNotNull(message);
        hearingId = message.getString("hearingId");


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );

    }


    @Test
    public void shouldCreateNewHearing_sendDefendantEmailNotification_NoCPSProsecutorNotification() throws IOException, JMSException, JSONException {
        final JmsMessageConsumerClient messageConsumerEmailRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();

        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID);

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;

        final JmsMessageConsumerClient messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.list-hearing-requested").getMessageConsumerClient();

        PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListHearingRequested, isJson(allOf(
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


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );

        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, CASE_ID);

    }

    @Test
    public void shouldCreateNewHearing_sendDefendantLetterNotification_ProsecutorEmailNotification() throws IOException, JMSException, JSONException {

        final JmsMessageConsumerClient messageConsumerEmailRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPrintRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.print-requested").getMessageConsumerClient();

        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps.json", randomUUID());
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation-no-email.json", DEFENDANT_ID);

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;

        final JmsMessageConsumerClient messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.list-hearing-requested").getMessageConsumerClient();

        PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListHearingRequested, isJson(allOf(
                withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)),
                withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)),
                withJsonPath("$.listNewHearing.bookingType", is("Video")),
                withJsonPath("$.listNewHearing.priority", is("High")),
                withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL"))
        )));
        assertNotNull(message);
        hearingId = message.getString("hearingId");


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );


        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, CASE_ID);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerPrintRequestPrivateEvent, CASE_ID);

    }

    private void doVerifyListHearingRequestedPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message.get(), notNullValue());
        final JsonObject progressionCommandNotificationEvent = message.get();
        assertThat(progressionCommandNotificationEvent.getString("caseId", EMPTY), is(caseId));

    }

}
