package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithTwoProsecutionCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.TIMEOUT_IN_SECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.time.Duration;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String extendedHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        doHearingConfirmedForOneDefendantAndTwoOffences(extendedHearingId, caseId, defendantId, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId1, defendantId1);
        final String existingHearingId = pollCaseAndGetHearingForDefendant(caseId1, defendantId1);

        // Extending hearing for one offence
        doHearingConfirmed(existingHearingId, caseId1, defendantId1, courtCentreId1, userId1, extendedHearingId);

        assertThat(queryAndVerifyHearingIsExtended(extendedHearingId, 2), is(true));
        verifyProbationHearingCommandInvoked(newArrayList(extendedHearingId, courtCentreId1));
    }

    @Test
    public void shouldPartiallyAllocateForOneProsecutionCaseWithTwoDefendantsToExistingHearing() throws Exception {
        // Allocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String userId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId1, defendantId1, defendantId2);
        final String extendedHearingId = pollCaseAndGetHearingForDefendant(caseId1, defendantId1);

        doHearingConfirmedForOneProsecutionCaseAndTwoDefendants(extendedHearingId, caseId1, defendantId1, defendantId2, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String defendantId4 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId2, defendantId3, defendantId4);
        final String existingHearingId = pollCaseAndGetHearingForDefendant(caseId2, defendantId3);

        // Extending hearing for one offence
        doHearingConfirmed(existingHearingId, caseId2, defendantId3, courtCentreId1, userId1, extendedHearingId);
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

        addProsecutionCaseToCrownCourtWithTwoProsecutionCases(caseId1, caseId2, defendantId1, defendantId2);
        final String extendedHearingId = pollCaseAndGetHearingForDefendant(caseId1, defendantId1);

        doHearingConfirmedForTwoProsecutionCaseAndOneDefendantEach(extendedHearingId, caseId1, caseId2, defendantId1, defendantId2, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId3 = randomUUID().toString();
        final String caseId4 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String defendantId4 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithTwoProsecutionCases(caseId3, caseId4, defendantId3, defendantId4);
        final String existingHearingId = pollCaseAndGetHearingForDefendant(caseId3, defendantId3);

        // Extending hearing for one offence
        doHearingConfirmed(existingHearingId, caseId3, defendantId3, courtCentreId1, userId1, extendedHearingId);
        await().atMost(TIMEOUT_IN_SECONDS, SECONDS).pollInterval(Duration.ofMillis(500)).until(() -> queryAndVerifyHearingIsExtended(extendedHearingId, 3));
    }

    private void doHearingConfirmedForOneDefendantAndTwoOffences(String hearingId, String caseId, String defendantId, String courtCentreId, String userId) {

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

    private void doHearingConfirmedForOneProsecutionCaseAndTwoDefendants(String hearingId, String caseId, String defendantId1, String defendantId2, String courtCentreId, String userId) {

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

    private void doHearingConfirmedForTwoProsecutionCaseAndOneDefendantEach(String hearingId, String caseId1, String caseId2, String defendantId1, String defendantId2, String courtCentreId, String userId) {

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

    private void doHearingConfirmed(String hearingId, String caseId, String defendantId, String courtCentreId, String userId, String extendedHearingId) {

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
