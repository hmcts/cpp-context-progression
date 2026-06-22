package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithTwoProsecutionCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.Utilities.sleepToBeRefactored;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class PartialAllocationOfHearingIT extends AbstractIT {

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_ONE_DEFENDANT_TWO_OFFENCES_FILE = "public.listing.hearing-confirmed-one-defendant-two-offences.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_ONE_CASE_TWO_DEFENDANT_FILE = "public.listing.hearing-confirmed-one-case-two-defendants.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_TWO_CASE_ONE_DEFENDANT_FILE = "public.listing.hearing-confirmed-two-cases-one-defendant.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID = "public.listing.hearing-confirmed-with-extended-hearing-id.json";

    @Test
    public void shouldPartiallyAllocateForOneDefendantWithTwoOffencesToExistingHearing() throws Exception {
        // Allocated hearing Id
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String userId = randomUUID().toString();

        // Defendant has 4 offences of which 2 are listed in a hearing
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String firstHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        doHearingConfirmedForOneDefendantAndTwoOffences(firstHearingId, caseId, defendantId, courtCentreId, userId);

        // UnAllocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId1, defendantId1);
        final String existingHearingId = pollCaseAndGetHearingForDefendant(caseId1, defendantId1);

        sleepToBeRefactored();
        // Extending hearing for one offence
        doHearingConfirmed(existingHearingId, caseId1, defendantId1, courtCentreId1, userId1, firstHearingId);

        pollAndVerifyHearingIsExtended(firstHearingId, 2);
        verifyProbationHearingCommandInvoked(newArrayList(firstHearingId, courtCentreId1));
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
        Thread.sleep(250);
        doHearingConfirmed(existingHearingId, caseId2, defendantId3, courtCentreId1, userId1, extendedHearingId);
        pollAndVerifyHearingIsExtended(extendedHearingId, 2);
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
        sleepToBeRefactored();
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
        sleepToBeRefactored();

        // Extending hearing for one offence
        doHearingConfirmed(existingHearingId, caseId3, defendantId3, courtCentreId1, userId1, extendedHearingId);
        pollAndVerifyHearingIsExtended(extendedHearingId, 3);
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

    private void pollAndVerifyHearingIsExtended(final String allocatedHearingId, final int numberOfProsecutionCases) {
        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(allocatedHearingId)),
                withJsonPath("$.hearing.prosecutionCases", hasSize(numberOfProsecutionCases))
        };
        pollForHearing(allocatedHearingId, hearingMatchers);
    }

    private void sendPublicEvent(final String eventName, final Metadata metadata, final JsonObject hearingConfirmedJson) {
        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(metadata, hearingConfirmedJson));
    }

}
