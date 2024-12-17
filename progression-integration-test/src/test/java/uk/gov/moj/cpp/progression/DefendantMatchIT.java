package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForExactMatchDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForLegalEntityDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForPartialMatchDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.unmatchDefendant;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchForCJSSpec;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchForSPISpec;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatchForCJSSpec;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatchForSPISpec;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchersForLegalEntity;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.stub.ListingStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class DefendantMatchIT extends AbstractIT {

    private static final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
    private static final JmsMessageConsumerClient publicEventConsumerForDefendantMatched = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-matched").getMessageConsumerClient();
    private static final JmsMessageConsumerClient publicEventConsumerForCaseDefendantChanged = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-defendant-changed").getMessageConsumerClient();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final JmsMessageConsumerClient publicEventConsumerForDefendantUnmatched = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-unmatched").getMessageConsumerClient();

    private static final JmsMessageConsumerClient privateEventConsumerForDefendantPartialMatchCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.defendant-partial-match-created").getMessageConsumerClient();
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String prosecutionCaseId_1;
    private String defendantId_1;
    private String masterDefendantId_1;
    private String prosecutionCaseId_2;
    private String defendantId_2;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String hearingId;
    private String courtCentreId;

    @BeforeEach
    public void setUp() {
        stubInitiateHearing();
        prosecutionCaseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        masterDefendantId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        hearingId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
    }

    @Test
    public void shouldMatchDefendant() throws IOException {
        // initiation of first case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final String hearingId = PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1);

        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);


        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_1, hearingId, defendantId_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForProsecutionCaseCreated();
        List<Matcher<? super ReadContext>> customMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDateCode", is(4))
        );

        Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, customMatchers);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        // initiation of second case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, defendantId_2, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final String hearingId2 = PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant(prosecutionCaseId_2, defendantId_2);
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId2);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        verifyInMessagingQueueForCaseDefendantChanged();

        // check master defendant id updated for defendant in case 2
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId_1))));
        verifyInMessagingQueueForDefendantMatched();
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

    }

    @Test
    public void shouldNotMatchLegalEntityDefendant() throws IOException {
        // initiation of first case
        initiateCourtProceedingsForLegalEntityDefendantMatching(prosecutionCaseId_1, defendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime);
        verifyInMessagingQueueForProsecutionCaseCreated();
        List<Matcher<? super ReadContext>> customMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDateCode", is(4))
        );

        Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchersForLegalEntity(prosecutionCaseId_1, defendantId_1, customMatchers);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        // initiation of second case
        initiateCourtProceedingsForLegalEntityDefendantMatching(prosecutionCaseId_2, defendantId_2, defendantId_2, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime);
        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchersForLegalEntity(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        verifyNoEventInMessagingQueueForCaseDefendantChanged();

    }

    @Test
    public void shouldUnmatchDefendant() throws IOException {
        // initiation of first case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final String hearingId = PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1);
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_1, hearingId, defendantId_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForProsecutionCaseCreated();
        Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        // initiation of second case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, defendantId_2, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final String hearingId2 = PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant(prosecutionCaseId_2, defendantId_2);
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId2);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_2, hearingId2, defendantId_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        verifyInMessagingQueueForCaseDefendantChanged();

        // check master defendant id updated for defendant in case 2
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId_1))));
        verifyInMessagingQueueForDefendantMatched();
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        //unmatch defendant 2
        unmatchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_2, defendantId_2, masterDefendantId_1);
        verifyInMessagingQueueForDefendantUnmatched();
        verifyInMessagingQueueForCaseDefendantChanged();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(defendantId_2))));
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);
    }

    @Test
    public void shouldRaiseDuplicatePublicEventWhenMatchedDefendantAlreadyBeenDeleted() throws IOException {

        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1, defendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, defendantId_2, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final String hearingId = PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant(prosecutionCaseId_2, defendantId_2);
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);

        // check master defendant id updated for defendant in case 2
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId_1))));
        verifyInMessagingQueueForDefendantMatched();
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // try to match again
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        verifyInMessagingQueueForDefendantMatched();
    }

    @Test
    public void updateHearingForMatchedDefendants() throws IOException {

        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, defendantId_2, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        final Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForDefendantMatched() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForDefendantMatched);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForDefendantUnmatched() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForDefendantUnmatched);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForCaseDefendantChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForCaseDefendantChanged);
        assertTrue(message.isPresent());
    }

    private void verifyNoEventInMessagingQueueForCaseDefendantChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(privateEventConsumerForDefendantPartialMatchCreated);
        assertFalse(message.isPresent());
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return new StringToJsonObjectConverter().convert(strPayload);
    }

    @Test
    public void shouldMatchDefendantForSPICase() throws IOException {
        shouldMatchDefendantsExactlyForCase("SPI", "20160000233W");
    }

    @Test
    public void shouldMatchDefendantForCPPICase() throws IOException {
        shouldMatchDefendantsExactlyForCase("CPPI", "2016/0000233W");
    }

    private void shouldMatchDefendantsExactlyForCase(final String caseType, final String pncId) throws IOException {
        stubUnifiedSearchQueryForExactDefendantMatching(prosecutionCaseId_1, defendantId_1);

        final String caseReceivedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(ZonedDateTime.now());
        initiateCourtProceedingsForExactMatchDefendants(prosecutionCaseId_1, defendantId_1, caseReceivedDate, caseType);
        verifyInMessagingQueueForProsecutionCaseCreated();

        final Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchersForExactMatch(pncId, "Louis");
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
    }

    private Matcher<? super ReadContext>[] getProsecutionCaseMatchersForExactMatch(final String pncId, final String lastName) {
        final List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is(pncId)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.lastName", is(lastName))
        );
        return matchers.toArray(new Matcher[0]);
    }

    private void stubUnifiedSearchQueryForExactDefendantMatching(final String caseId, final String defendantId) {
        stubUnifiedSearchQueryExactMatchForSPISpec(caseId, defendantId);
        stubUnifiedSearchQueryExactMatchForCJSSpec(caseId, defendantId);
    }

    @Test
    public void shouldMatchDefendantPartiallyForSPICase() throws IOException {
        shouldMatchDefendantsPartiallyForCase("SPI", "20160000234W");
    }

    @Test
    public void shouldMatchDefendantPartiallyForCPPICase() throws IOException {
        shouldMatchDefendantsPartiallyForCase("CPPI", "2016/0000234W");
    }

    private Matcher<? super ReadContext>[] getProsecutionCaseMatchersForPartialMatch(final String pncId) {
        final List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is(pncId))
        );
        return matchers.toArray(new Matcher[0]);
    }

    private void shouldMatchDefendantsPartiallyForCase(final String caseType, final String pncId) throws IOException {
        stubUnifiedSearchQueryForPartialDefendantMatching(prosecutionCaseId_1, defendantId_1);

        final String caseReceivedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(ZonedDateTime.now());
        initiateCourtProceedingsForPartialMatchDefendants(prosecutionCaseId_1, defendantId_1, caseReceivedDate, caseType);
        verifyInMessagingQueueForProsecutionCaseCreated();

        final Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchersForPartialMatch(pncId);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
    }

    private void stubUnifiedSearchQueryForPartialDefendantMatching(final String caseId, final String defendantId) {
        stubUnifiedSearchQueryPartialMatchForSPISpec(caseId, defendantId);
        stubUnifiedSearchQueryPartialMatchForCJSSpec(caseId, defendantId);
    }
}

