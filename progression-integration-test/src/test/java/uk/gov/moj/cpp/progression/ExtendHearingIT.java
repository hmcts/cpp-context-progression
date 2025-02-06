package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefenceCounsel;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;


import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.gov.justice.services.messaging.Metadata;
import java.util.UUID;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1607")
public class ExtendHearingIT extends AbstractIT {

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID = "public.listing.hearing-confirmed-with-extended-hearing-id.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED = "public.events.listing.allocated-hearing-deleted";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendHearingIT.class);
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

    private static String caseId;
    private static String defendantId;
    private static String courtCentreId;
    private static String userId;
    private static String caseId1;
    private static String defendantId1;
    private static String courtCentreId1;
    private static String userId1;


    @BeforeAll
    public static void setUp() {
        HearingStub.stubInitiateHearing();
        stubDocumentCreate(STRING.next());
    }

    @BeforeEach
    public void setUpBeforeTest() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        userId = randomUUID().toString();
        caseId1 = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        courtCentreId1 = randomUUID().toString();
        userId1 = randomUUID().toString();
    }


    @Test
    public void shouldAddNewCaseToUnallocatedHearingWhenExtending() throws Exception{
        final UUID offenceId = randomUUID();
        final UUID  offenceId1 = randomUUID();
        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId,offenceId );
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String extendedHearingId = retrieveMessageAsJsonPath(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(CoreMatchers.allOf(withJsonPath("$.hearing.prosecutionCases[0].id", CoreMatchers.is(caseId))))).getJsonObject("hearing.id");
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId, defendantId );

        doHearingConfirmedAndVerify(extendedHearingId,caseId,defendantId,courtCentreId,userId);
        verifyListingNumberForCase(caseId,defendantId, 1);
        doVerifyProsecutionCaseDefendantListingStatusChanged();


        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId1, defendantId1,offenceId1 );
        pollProsecutionCasesProgressionFor(caseId1, getProsecutionCaseMatchers(caseId1, defendantId1));
        final String existingHearingId = retrieveMessageAsJsonPath(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(CoreMatchers.allOf(withJsonPath("$.hearing.prosecutionCases[0].id", CoreMatchers.is(caseId1))))).getJsonObject("hearing.id");
        doHearingConfirmedAndVerify(existingHearingId,caseId1,defendantId1,courtCentreId,userId, offenceId1);
        doVerifyProsecutionCaseDefendantListingStatusChanged();
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId1, defendantId1 );
        poll(requestParams(getReadUrl("/hearingSearch/" + existingHearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearing.prosecutionCases.length()", CoreMatchers.is(1))
                        ));

        final JsonObject payload = createObjectBuilder()
                .add("existingHearingId", extendedHearingId)
                .add("hearingId", existingHearingId)
                .add("confirmedProsecutionCase", createArrayBuilder()
                        .add(createObjectBuilder().add("id", caseId)
                                .add("defendants", createArrayBuilder().add(createObjectBuilder().add("id", defendantId)
                                                .add("offences", createArrayBuilder().add(createObjectBuilder().add("id", offenceId.toString()).build())
                                                        .build()).build())
                                        .build())
                                .build())
                        .build())
                .build();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata("public.listing.cases-added-to-hearing", userId), payload);
        messageProducerClientPublic.sendMessage("public.listing.cases-added-to-hearing", publicEventEnvelope);

        poll(requestParams(getReadUrl("/hearingSearch/" + existingHearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearing.prosecutionCases.length()", CoreMatchers.is(2))
                        ));

        final JsonObject payload2 = createObjectBuilder().add("hearingId", existingHearingId)
                .add("offenceIds", createArrayBuilder().add("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").add(offenceId1.toString()))
                .build();

        final JsonEnvelope publicEventEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata("public.events.listing.offences-removed-from-unallocated-hearing", userId), payload2);
        messageProducerClientPublic.sendMessage("public.events.listing.offences-removed-from-unallocated-hearing", publicEventEnvelope2);

        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {} | offenceId : {}", existingHearingId, caseId1, defendantId1, offenceId1 );

        poll(requestParams(getReadUrl("/hearingSearch/" + existingHearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearing.prosecutionCases.length()", CoreMatchers.is(1))
                        ));

        final List<String> attendanceDaysList = Stream.of(FUTURE_LOCAL_DATE.next().toString(), FUTURE_LOCAL_DATE.next().toString()).collect(Collectors.toList());
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed").getMessageConsumerClient();
            addDefenceCounsel(existingHearingId, randomUUID().toString(), Stream.of(defendantId1).collect(Collectors.toList()),  attendanceDaysList, "progression.add-hearing-defence-counsel.json");
            assertNotNull(retrieveMessageAsJsonPath(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(CoreMatchers.allOf(withJsonPath("$.hearing.prosecutionCases.length()", CoreMatchers.is(1))))));


    }

    @Test
    public void shouldIncreaseListingNumberWhenHearingDeletedForProsecutionCases() throws Exception {

        doReferCaseToCourtAndVerify(caseId, defendantId);
        final String extendedHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId, defendantId);

        doHearingConfirmedAndVerify(extendedHearingId, caseId, defendantId, courtCentreId, userId);
        verifyListingNumberForCase(caseId, defendantId, 1);

        // UnAllocated hearing Id
        doReferCaseToCourtAndVerify(caseId1, defendantId1);
        final String existingHearingId = pollCaseAndGetHearingForDefendant(caseId1, defendantId1);
        doHearingConfirmedAndVerify(existingHearingId, caseId, defendantId, courtCentreId, userId);
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId1, defendantId1);

        // Extending hearing
        doHearingConfirmedAndVerify(existingHearingId, caseId1, defendantId1, courtCentreId1, userId1, extendedHearingId);

        queryAndVerifyHearingIsExtended(extendedHearingId, 2);

        verifyListingNumberForCase(caseId, defendantId, 2);

        //delete Hearing
        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(extendedHearingId);
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED, userId), hearingDeletedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED, publicEventEnvelope);

        verifyHearingIsEmpty(extendedHearingId);
        verifyListingNumberForCase(caseId, defendantId, 1);
    }


    private void doReferCaseToCourtAndVerify(final String caseId, final String defendantId) throws IOException, JSONException {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    private void verifyListingNumberForCase(final String caseId, final String defendantId, final int listingNumber) {
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].listingNumber", CoreMatchers.is(listingNumber))
        )));
    }

    private void doHearingConfirmedAndVerify(String hearingId, String caseId, String defendantId, String courtCentreId, String userId) {

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_FILE)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

    }

    private void doHearingConfirmedAndVerify(String hearingId, String caseId, String defendantId, String courtCentreId, String userId, String extendedHearingId) {

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("EXTENDED_ID", extendedHearingId));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

    }

    private void doHearingConfirmedAndVerify(String hearingId, String caseId, String defendantId, String courtCentreId, String userId, UUID offenceId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed-one-defendant-two-offences-ids.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("RANDOM_OFFENCE_ID_2", offenceId.toString())
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String resultHearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(resultHearingId, new Matcher[0]);
        LOGGER.info("Hearing Id for defendant listing status changed : {} ", resultHearingId);
        return resultHearingId;
    }

    private void queryAndVerifyHearingIsExtended(final String allocatedHearingId, final int numberOfProsecutionCases) {
        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(allocatedHearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", CoreMatchers.is(1)),
                withJsonPath("$.hearing.prosecutionCases[1].defendants[0].offences[0].listingNumber", CoreMatchers.is(1))
        };

        final String dbHearing = pollForResponse("/hearingSearch/" + allocatedHearingId, PROGRESSION_QUERY_HEARING_JSON, hearingMatchers);
        final JsonObject hearingExtendedJsonObject = stringToJsonObjectConverter.convert(dbHearing);
        assertThat(hearingExtendedJsonObject.getJsonObject("hearing")
                .getJsonArray("prosecutionCases").size(), is(numberOfProsecutionCases));
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
    }
}
