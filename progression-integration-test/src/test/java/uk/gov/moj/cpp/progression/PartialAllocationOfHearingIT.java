package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithTwoProsecutionCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.TIMEOUT;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.time.Duration;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(JmsResourceManagementExtension.class)
@SuppressWarnings("squid:S1607")
public class PartialAllocationOfHearingIT {

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_ONE_DEFENDANT_TWO_OFFENCES_FILE = "public.listing.hearing-confirmed-one-defendant-two-offences.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_ONE_CASE_TWO_DEFENDANT_FILE = "public.listing.hearing-confirmed-one-case-two-defendants.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_TWO_CASE_ONE_DEFENDANT_FILE = "public.listing.hearing-confirmed-two-cases-one-defendant.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID = "public.listing.hearing-confirmed-with-extended-hearing-id.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final Logger LOGGER = LoggerFactory.getLogger(PartialAllocationOfHearingIT.class);
    private static final JmsMessageConsumerClient messageConsumerProgressionHearingExtendedEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-extended").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();


    @BeforeAll
    public static void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @Test
    public void shouldPartiallyAllocateForOneDefendantWithTwoOffencesToExistingHearing() throws Exception {
        // Allocated hearing Id
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String userId = randomUUID().toString();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String extendedHearingId = doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(caseId, messageConsumerProsecutionCaseDefendantListingStatusChanged);
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId, defendantId);

        doHearingConfirmedAndVerifyForOneDefendantAndTwoOffences(extendedHearingId, caseId, defendantId, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId1, defendantId1);
        pollProsecutionCasesProgressionFor(caseId1, getProsecutionCaseMatchers(caseId1, defendantId1));

        final String existingHearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId1, defendantId1, getProsecutionCaseMatchers(caseId1, defendantId1));
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId1, defendantId1);

        // Extending hearing for one offence
        doHearingConfirmedAndVerify(existingHearingId, caseId1, defendantId1, courtCentreId1, userId1, extendedHearingId);

        doVerifyProgressionHearingExtendedEvent(extendedHearingId, caseId1);
        assertThat(queryAndVerifyHearingIsExtended(extendedHearingId, 2), is(true));
        final JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(extendedHearingId)),
                withJsonPath("$.hearing.prosecutionCases", hasSize(2)),
                withJsonPath("$.hearing.courtCentre.id", CoreMatchers.is(courtCentreId1))
        )));
        assertThat(messageDaysMatchers, notNullValue());
    }

    @Test
    public void shouldPartiallyAllocateForOneProsecutionCaseWithTwoDefendantsToExistingHearing() throws Exception {
        // Allocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId1, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId1, getProsecutionCaseMatchers(caseId1, defendantId1));

        final String extendedHearingId = doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(caseId1, messageConsumerProsecutionCaseDefendantListingStatusChanged);
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId1, defendantId1);

        doHearingConfirmedAndVerifyForOneProsecutionCaseAndTwoDefendants(extendedHearingId, caseId1, defendantId1, defendantId2, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String defendantId4 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId2, defendantId3, defendantId4);
        pollProsecutionCasesProgressionFor(caseId2, getProsecutionCaseMatchers(caseId2, defendantId3));

        final String existingHearingId = doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(caseId2, messageConsumerProsecutionCaseDefendantListingStatusChanged2);
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId2, defendantId3);

        // Extending hearing for one offence
        doHearingConfirmedAndVerify(existingHearingId, caseId2, defendantId3, courtCentreId1, userId1, extendedHearingId);

        doVerifyProgressionHearingExtendedEvent(extendedHearingId, caseId2);
        assertThat(queryAndVerifyHearingIsExtended(extendedHearingId, 2), is(true));
    }

    @Test
    public void shouldPartiallyAllocateTwoProsecutionCasesToExistingHearing() throws Exception {
        // Allocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String caseId2 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String userId = randomUUID().toString();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithTwoProsecutionCases(caseId1, caseId2, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId1, getProsecutionCaseMatchers(caseId1, defendantId1));

        final String extendedHearingId = doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(caseId1, caseId2, messageConsumerProsecutionCaseDefendantListingStatusChanged);
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId1, defendantId1);

        doHearingConfirmedAndVerifyForTwoProsecutionCaseAndOneDefendants(extendedHearingId, caseId1, caseId2, defendantId1, defendantId2, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId3 = randomUUID().toString();
        final String caseId4 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String defendantId4 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithTwoProsecutionCases(caseId3, caseId4, defendantId3, defendantId4);
        pollProsecutionCasesProgressionFor(caseId3, getProsecutionCaseMatchers(caseId3, defendantId3));

        final String existingHearingId = doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(caseId3, caseId4, messageConsumerProsecutionCaseDefendantListingStatusChanged);
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId2, defendantId3);

        // Extending hearing for one offence
        doHearingConfirmedAndVerify(existingHearingId, caseId3, defendantId3, courtCentreId1, userId1, extendedHearingId);

        doVerifyProgressionHearingExtendedEvent(extendedHearingId, caseId3);
        await().atMost(TIMEOUT, SECONDS).pollInterval(Duration.ofMillis(500)).until(() -> queryAndVerifyHearingIsExtended(extendedHearingId, 3)
        );
    }

    private void doHearingConfirmedAndVerifyForOneDefendantAndTwoOffences(String hearingId, String caseId, String defendantId, String courtCentreId, String userId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_FOR_ONE_DEFENDANT_TWO_OFFENCES_FILE)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

    }

    private void doHearingConfirmedAndVerifyForOneProsecutionCaseAndTwoDefendants(String hearingId, String caseId, String defendantId1, String defendantId2, String courtCentreId, String userId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_FOR_ONE_CASE_TWO_DEFENDANT_FILE)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1)
                        .replaceAll("DEFENDANT_ID_2", defendantId2)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

    }

    private void doHearingConfirmedAndVerifyForTwoProsecutionCaseAndOneDefendants(String hearingId, String caseId1, String caseId2, String defendantId1, String defendantId2, String courtCentreId, String userId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_FOR_TWO_CASE_ONE_DEFENDANT_FILE)
                        .replaceAll("CASE_ID_1", caseId1)
                        .replaceAll("CASE_ID_2", caseId2)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1)
                        .replaceAll("DEFENDANT_ID_2", defendantId2)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

    }

    private void doHearingConfirmedAndVerify(String hearingId, String caseId, String defendantId, String courtCentreId, String userId, String extendedHearingId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("EXTENDED_ID", extendedHearingId));

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(final String caseId1, final String caseId2, final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(
                withJsonPath("$.hearing.prosecutionCases[0].id", is(oneOf(caseId1, caseId2)))
        ));
        final String resultHearingId = message.getJsonObject("hearing.id");
        getHearingForDefendant(resultHearingId, new Matcher[0]);
        LOGGER.info("Hearing Id for defendant listing status changed : {} ", resultHearingId);
        return resultHearingId;
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChangedForCaseId(final String caseId, final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId))
        ));
        final String resultHearingId = message.getJsonObject("hearing.id");
        getHearingForDefendant(resultHearingId, new Matcher[0]);
        LOGGER.info("Hearing Id for defendant listing status changed : {} ", resultHearingId);
        return resultHearingId;
    }

    private void doVerifyProgressionHearingExtendedEvent(final String allocatedHearingId, final String caseId) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerProgressionHearingExtendedEvent, isJson(Matchers.allOf(
                        withJsonPath("$.hearingRequest.id", is(allocatedHearingId)),
                        withJsonPath("$.hearingRequest.prosecutionCases[0].id", is(caseId))
                ))
        );
        assertThat(message, notNullValue());
    }

    private boolean queryAndVerifyHearingIsExtended(final String allocatedHearingId, final int numberOfProsecutionCases) {
        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(allocatedHearingId))
        };
        final String dbHearing = pollForResponse("/hearingSearch/" + allocatedHearingId, PROGRESSION_QUERY_HEARING_JSON, hearingMatchers);
        final JsonObject hearingExtendedJsonObject = stringToJsonObjectConverter.convert(dbHearing);
        return numberOfProsecutionCases == hearingExtendedJsonObject.getJsonObject("hearing")
                .getJsonArray("prosecutionCases").size();
    }

    private static void sendPublicEvent(final String eventName, final Metadata metadata, final JsonObject hearingConfirmedJson) {
        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(metadata, hearingConfirmedJson));
    }

}
