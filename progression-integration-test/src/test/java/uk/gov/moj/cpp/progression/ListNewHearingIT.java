package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.DefenceCounselIT.PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED;
import static uk.gov.moj.cpp.progression.DefenceCounselIT.getDefenceCounselPublicEventPayload;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtFirstHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithCommittingCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.listNewHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListNewHearingIT extends AbstractIT {

    private static final String PROGRESSION_EVENT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private String prosecutorEmail;

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();
        prosecutorEmail = randomAlphanumeric(15) + "@email.com";
    }

    @Test
    public void shouldCreateNewHearing_sendDefendantLetterNotification_ProsecutorEmailNotification() throws IOException, JSONException {

        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String prosecutionAuthorityId = randomUUID().toString();

        stubProsecutorData(prosecutorEmail, prosecutionAuthorityId);
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation-no-email.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, prosecutionAuthorityId, generateUrn(), defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        listNewHearing(caseId, defendantId);

        verifyPostListCourtHearing(caseId, defendantId, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForHearing(hearingId, withJsonPath("$.hearing.id", is(hearingId)), withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")));

        verifyEmailNotificationIsRaisedWithAttachment(newArrayList(prosecutorEmail));
        verifyCreateLetterRequested(newArrayList("postage", "first", "letterUrl"));

    }

    @Test
    public void shouldExtendAdhocHearingToExistingHearing() throws IOException, JSONException {
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

        final JsonObject hearingAddDefenceCounselJson = getDefenceCounselPublicEventPayload(hearingPayload.getString("id"), "Harry");
        final JsonEnvelope publicEventAddedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED,  randomUUID().toString()), hearingAddDefenceCounselJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED, publicEventAddedEnvelope);

        pollForHearing(hearingPayload.getString("id"),
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingPayload.getString("id"))),
                withJsonPath("$.hearing.defenceCounsels[0].id", CoreMatchers.is("fab947a3-c50c-4dbb-accf-b2758b1d2d6d"))
        );

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

        pollForHearing(hearingPayload.getString("id"),
                withJsonPath("$.hearing.id", is(hearingPayload.getString("id"))),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")),
                withJsonPath("$.hearing.prosecutionCases.length()", is(2)),
                withJsonPath("$.hearing.defenceCounsels[0].id", CoreMatchers.is("fab947a3-c50c-4dbb-accf-b2758b1d2d6d"))
        );

    }

    @Test
    void shouldAddCaseToHearingBDF() throws IOException, JSONException {
        final String CASE_ID = randomUUID().toString();
        final String DEFENDANT_ID = randomUUID().toString();

        final String CASE_ID2 = randomUUID().toString();
        final String DEFENDANT_ID2 = randomUUID().toString();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID);
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", DEFENDANT_ID2);

        final JsonObject hearingPayload;

        JmsMessageConsumerClient messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();
        initiateCourtProceedingsWithCommittingCourt(CASE_ID, DEFENDANT_ID, ZonedDateTimes.fromString("2024-05-30T18:32:04.238Z").toString(), ZonedDateTimes.fromString("2024-05-30T18:32:04.238Z").toString());

        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID, singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));
        final JsonObject payload = retrieveMessageBody(messageConsumerListHearingRequested).get();
        hearingPayload = payload.getJsonObject("hearing");

        messageConsumerListHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();

        addProsecutionCaseToCrownCourtFirstHearing(CASE_ID2, DEFENDANT_ID2, false);

        pollProsecutionCasesProgressionFor(CASE_ID2, getProsecutionCaseMatchers(CASE_ID2, DEFENDANT_ID2, singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final String offenceId = retrieveMessageBody(messageConsumerListHearingRequested).get().getJsonObject("hearing")
                .getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id");

        pollForHearing(hearingPayload.getString("id"), withJsonPath("$.hearing.id", is(hearingPayload.getString("id"))), withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")), withJsonPath("$.hearing.prosecutionCases.length()", is(1)));

        JsonObjectBuilder payloadBuilder = createObjectBuilder().add("hearingId",hearingPayload.getString("id") )
                .add("casesBdf", createArrayBuilder().add(createObjectBuilder().add("caseId", CASE_ID2)
                        .add("defendantsBdf", createArrayBuilder().add(createObjectBuilder().add("defendantId", DEFENDANT_ID2)
                                .add("offences", createArrayBuilder().add(offenceId).build())).build())));

        JmsMessageConsumerClient messageConsumerBdfPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.case-added-to-hearing-bdf").getMessageConsumerClient();

        postCommand(getWriteUrl("/hearing/"+hearingPayload.getString("id")), "application/vnd.progression.add-case-to-hearing-bdf+json", payloadBuilder.build().toString());

        doVerifyBdfPrivateEvent(messageConsumerBdfPrivateEvent, CASE_ID, CASE_ID2);

        pollForHearing(hearingPayload.getString("id"), withJsonPath("$.hearing.id", is(hearingPayload.getString("id"))), withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")), withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES")), withJsonPath("$.hearing.prosecutionCases.length()", is(2)));

    }

    private JsonObjectBuilder prepareAdhocHearingPayload(final JsonObject hearingPayload, final JsonObject listHearingRequests, final JsonArray casePayload) {
        JsonObjectBuilder listNewHearingBuilder = createObjectBuilder();
        listNewHearingBuilder.add("id", hearingPayload.getString("id"));
        listNewHearingBuilder.add("hearingType", hearingPayload.getJsonObject("type"));
        listNewHearingBuilder.add("jurisdictionType", hearingPayload.getString("jurisdictionType"));
        listNewHearingBuilder.add("courtCentre", hearingPayload.getJsonObject("courtCentre"));
        listNewHearingBuilder.add("estimatedMinutes", listHearingRequests.get("estimateMinutes"));
        listNewHearingBuilder.add("earliestStartDateTime", listHearingRequests.get("earliestStartDateTime"));

        JsonArrayBuilder listDefendantRequestsBuilder = createArrayBuilder();
        casePayload.stream().map(jv -> (JsonObject) jv).forEach(pc -> pc.getJsonArray("defendants").stream().map(jv -> (JsonObject) jv).forEach(def -> {
            JsonObjectBuilder listDefendantRequestBuilder = createObjectBuilder();
            listDefendantRequestBuilder.add("prosecutionCaseId", pc.getString("id"));
            listDefendantRequestBuilder.add("defendantId", def.getString("id"));
            JsonArrayBuilder defendantOffences = createArrayBuilder();
            def.getJsonArray("offences").stream().map(jv -> (JsonObject) jv).forEach(off -> defendantOffences.add(off.getString("id")));
            listDefendantRequestBuilder.add("defendantOffences", defendantOffences);
            listDefendantRequestsBuilder.add(listDefendantRequestBuilder);
        }));
        listNewHearingBuilder.add("listDefendantRequests", listDefendantRequestsBuilder);
        JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add("listNewHearing", listNewHearingBuilder);
        payloadBuilder.add("sendNotificationToParties", true);
        return payloadBuilder;
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

    private void doVerifyBdfPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId, final String caseId2) {
        final Optional<JsonObject> message1 = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message1.get(), notNullValue());
        assertThat(message1.get().getJsonArray("prosecutionCases").size(), is(1));
        assertThat(message1.get().getJsonArray("prosecutionCases").getJsonObject(0).getString("id"), is(caseId2));

    }

    private void stubProsecutorData(final String prosecutorEmail, final String prosecutionAuthorityId) {
        final String payload = getPayload("restResource/referencedata.query.prosecutor-noncps-random-email.json")
                .replaceAll("RANDOM_EMAIL", prosecutorEmail)
                .replaceAll("RANDOM_PROSECUTOR_ID", prosecutionAuthorityId);
        final JsonObject payloadAsJsonObject = new StringToJsonObjectConverter().convert(payload);
        stubQueryProsecutorData(payloadAsJsonObject, fromString(prosecutionAuthorityId), randomUUID());
    }

    public Response addProsecutionCaseToCrownCourt(final String caseId, final String prosecutionAuthorityId, final String caseUrn, final String defendantId) {

        final String payload = getPayload("progression.command.prosecution-case-refer-to-court-random-prosecutor.json")
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_ID", prosecutionAuthorityId)
                .replaceAll("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);

        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                payload);
    }

}
