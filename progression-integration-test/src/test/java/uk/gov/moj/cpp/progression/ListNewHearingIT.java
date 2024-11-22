package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtFirstHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithCommittingCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.hamcrest.CoreMatchers;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

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
    private static final String PROGRESSION_EVENT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";

    @BeforeEach
    public void setUp() {
        stubInitiateHearing();
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

        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListHearingRequested, isJson(allOf(withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)), withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)), withJsonPath("$.listNewHearing.bookingType", is("Video")), withJsonPath("$.listNewHearing.priority", is("High")), withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)), withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL")))));
        assertNotNull(message);
        hearingId = message.getString("hearingId");


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, withJsonPath("$.hearing.id", is(hearingId)), withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")));

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

        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListHearingRequested, isJson(allOf(withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)), withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)), withJsonPath("$.listNewHearing.bookingType", is("Video")), withJsonPath("$.listNewHearing.priority", is("High")), withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)), withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL")), withJsonPath("$.sendNotificationToParties", is(true)))));
        assertNotNull(message);
        hearingId = message.getString("hearingId");


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, withJsonPath("$.hearing.id", is(hearingId)), withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")));

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

        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerListHearingRequested, isJson(allOf(withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)), withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)), withJsonPath("$.listNewHearing.bookingType", is("Video")), withJsonPath("$.listNewHearing.priority", is("High")), withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)), withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL")))));
        assertNotNull(message);
        hearingId = message.getString("hearingId");


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, withJsonPath("$.hearing.id", is(hearingId)), withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")));


        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, CASE_ID);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerPrintRequestPrivateEvent, CASE_ID);

    }


    @Test
    public void shouldExtendAdhocHearingToExistingHearing() throws IOException, JMSException, JSONException {
        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        final String CASE_ID2 = randomUUID().toString();
        final String DEFENDANT_ID2 = randomUUID().toString();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID);
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID2);

        final JsonObject hearingPayload;
        final JsonObject listHearingRequests;

        JmsMessageConsumerClient messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();
        initiateCourtProceedingsWithCommittingCourt(CASE_ID, DEFENDANT_ID, ZonedDateTimes.fromString("2024-05-30T18:32:04.238Z").toString(), ZonedDateTimes.fromString("2024-05-30T18:32:04.238Z").toString());

        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID, singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));
        final JsonObject payload = retrieveMessageBody(messageConsumerListHearingRequested).get();
        hearingPayload = payload.getJsonObject("hearing");
        listHearingRequests = payload.getJsonArray("listHearingRequests").getJsonObject(0);

        messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();

        addProsecutionCaseToCrownCourtFirstHearing(CASE_ID2, DEFENDANT_ID2, false);

        pollProsecutionCasesProgressionFor(CASE_ID2, getProsecutionCaseMatchers(CASE_ID2, DEFENDANT_ID2, singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final JsonArray casePayload = retrieveMessageBody(messageConsumerListHearingRequested).get().getJsonObject("hearing").getJsonArray("prosecutionCases");


        JsonObjectBuilder payloadBuilder = prepareAdhocHearingPayload(hearingPayload, listHearingRequests, casePayload);

        final JmsMessageConsumerClient publicEvent = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.related-hearing-updated-for-adhoc-hearing").getMessageConsumerClient();

        JmsMessageConsumerClient messageConsumerEmailRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();

        postCommand(getWriteUrl("/listnewhearing"), "application/vnd.progression.list-new-hearing+json", payloadBuilder.build().toString());

        final JsonPath message = retrieveMessageAsJsonPath(publicEvent, isJson(allOf(withJsonPath("$.prosecutionCases.length()", is(2)), withJsonPath("$.hearingId", is(hearingPayload.getString("id"))))));
        assertNotNull(message);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, CASE_ID, CASE_ID2);


        pollForResponse("/hearingSearch/" + hearingPayload.getString("id"), PROGRESSION_QUERY_HEARING_JSON, withJsonPath("$.hearing.id", is(hearingPayload.getString("id"))), withJsonPath("$.hearingListingStatus", is("HEARING_INITIALISED")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")), withJsonPath("$.hearing.prosecutionCases.length()", is(2)));

    }

    private JsonObjectBuilder prepareAdhocHearingPayload(final JsonObject hearingPayload, final JsonObject listHearingRequests, final JsonArray casePayload) {
        JsonObjectBuilder listNewHearingBuilder = Json.createObjectBuilder();
        listNewHearingBuilder.add("id", hearingPayload.getString("id"));
        listNewHearingBuilder.add("hearingType", hearingPayload.getJsonObject("type"));
        listNewHearingBuilder.add("jurisdictionType", hearingPayload.getString("jurisdictionType"));
        listNewHearingBuilder.add("courtCentre", hearingPayload.getJsonObject("courtCentre"));
        listNewHearingBuilder.add("estimatedMinutes", listHearingRequests.get("estimateMinutes"));
        listNewHearingBuilder.add("earliestStartDateTime", listHearingRequests.get("earliestStartDateTime"));

        JsonArrayBuilder listDefendantRequestsBuilder = Json.createArrayBuilder();
        casePayload.stream().map(jv -> (JsonObject) jv).forEach(pc -> pc.getJsonArray("defendants").stream().map(jv -> (JsonObject) jv).forEach(def -> {
            JsonObjectBuilder listDefendantRequestBuilder = Json.createObjectBuilder();
            listDefendantRequestBuilder.add("prosecutionCaseId", pc.getString("id"));
            listDefendantRequestBuilder.add("defendantId", def.getString("id"));
            JsonArrayBuilder defendantOffences = Json.createArrayBuilder();
            def.getJsonArray("offences").stream().map(jv -> (JsonObject) jv).forEach(off -> defendantOffences.add(off.getString("id")));
            listDefendantRequestBuilder.add("defendantOffences", defendantOffences);
            listDefendantRequestsBuilder.add(listDefendantRequestBuilder);
        }));
        listNewHearingBuilder.add("listDefendantRequests", listDefendantRequestsBuilder);
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payloadBuilder.add("listNewHearing", listNewHearingBuilder);
        payloadBuilder.add("sendNotificationToParties", true);
        return payloadBuilder;
    }

    private void doVerifyListHearingRequestedPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message.get(), notNullValue());
        final JsonObject progressionCommandNotificationEvent = message.get();
        assertThat(progressionCommandNotificationEvent.getString("caseId", EMPTY), is(caseId));

    }

    private void doVerifyListHearingRequestedPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId, final String caseId2) {
        final Optional<JsonObject> message1 = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message1.get(), notNullValue());
        final Optional<JsonObject> message2 = QueueUtil.retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message2.get(), notNullValue());
        assertThat(message1.get().getString("caseId"), anyOf(is(caseId), is(caseId2)));
        assertThat(message2.get().getString("caseId"), anyOf(is(caseId), is(caseId2)));
        assertThat(message1.get().getString("caseId"), not(is(message2.get().getString("caseId"))));

    }
}
