package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtNullPostCode;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.ArrayList;
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
    private static final JmsMessageConsumerClient messageConsumerProgressionHearingExtendedEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-extended").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerProgressionSummonsDataPreparedEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.summons-data-prepared").getMessageConsumerClient();

    private static List<String> caseIds = new ArrayList<>();
    private static List<String> defendantIds = new ArrayList<>();
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
        caseIds.add(caseId);
        defendantIds.add(defendantId);
    }

    @Test
    public void shouldIncreaseListingNumberWhenHearingDeletedForProsecutionCases() throws Exception {

        doReferCaseToCourtAndVerify(caseId, defendantId);
        final String extendedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId, defendantId);

        doHearingConfirmedAndVerify(extendedHearingId, caseId, defendantId, courtCentreId, userId);
        verifyListingNumberForCase(caseId, defendantId, 1);
        doVerifyProsecutionCaseDefendantListingStatusChanged();

        // UnAllocated hearing Id
        caseIds.add(caseId1);
        defendantIds.add(defendantId1);

        doReferCaseToCourtAndVerify(caseId1, defendantId1);
        final String existingHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        doHearingConfirmedAndVerify(existingHearingId, caseId, defendantId, courtCentreId, userId);
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId1, defendantId1);

        // Extending hearing
        doHearingConfirmedAndVerify(existingHearingId, caseId1, defendantId1, courtCentreId1, userId1, extendedHearingId);

        doVerifyProgressionHearingExtendedEvent(extendedHearingId, caseId1);
        queryAndVerifyHearingIsExtended(extendedHearingId, 2);
        queryAndVerifyHearingIsExisting(existingHearingId);

        doVerifyProgressionSummonsDataPreparedEvent(caseIds, defendantIds);
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

    private void verifyListingNumberForCase(final String caseId, final String defendantId, final int listingNumber) throws IOException {
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

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String resultHearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(resultHearingId, new Matcher[0]);
        LOGGER.info("Hearing Id for defendant listing status changed : {} ", resultHearingId);
        return resultHearingId;
    }

    private void doVerifyProgressionHearingExtendedEvent(final String allocatedHearingId, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionHearingExtendedEvent);
        final JsonObject extendHearingCommand = message.get();
        assertThat(extendHearingCommand.getJsonObject("hearingRequest").getString("id"), equalTo(allocatedHearingId));
        assertThat(extendHearingCommand.getJsonObject("hearingRequest")
                .getJsonArray("prosecutionCases").getJsonObject(0).getString("id"), is(caseId));
    }

    private void doVerifyProgressionSummonsDataPreparedEvent(final List<String> caseIds, final List<String> defendantIds) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionSummonsDataPreparedEvent);
        final JsonObject extendHearingCommand = message.get();

        assertThat(caseIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("confirmedProsecutionCaseIds")
                .getJsonObject(0).getString("id")));
        assertThat(defendantIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("confirmedProsecutionCaseIds")
                .getJsonObject(0).getJsonArray("confirmedDefendantIds").getString(0)));

        assertThat(caseIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("listDefendantRequests")
                .getJsonObject(0).getString("prosecutionCaseId")));

        assertThat(defendantIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("listDefendantRequests")
                .getJsonObject(0).getJsonObject("referralReason").getString("defendantId")));
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

    private void queryAndVerifyHearingIsExisting(final String allocatedHearingId) {
        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", CoreMatchers.is(2))
        };
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
    }
}
