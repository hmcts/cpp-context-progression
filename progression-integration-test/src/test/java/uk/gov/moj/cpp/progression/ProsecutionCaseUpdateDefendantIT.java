package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;


public class ProsecutionCaseUpdateDefendantIT extends AbstractIT {

    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PROGRESSION_QUERY_CASE_LSM_INFO = "application/vnd.progression.query.case-lsm-info+json";
    ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String defendantId;
    private MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
            .createPublicConsumer("public.progression.prosecution-case-created");
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



    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        stubInitiateHearing();
        prosecutionCaseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        masterDefendantId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        hearingId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() throws Exception {
        // given
        final String policeBailStatusId = randomUUID().toString();
        final String policeBailStatusDesc = "police bail status description";
        final String policeBailConditions = "police bail conditions";

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendantWithPoliceBailInfo(policeBailStatusId, policeBailStatusDesc, policeBailConditions);

        // then
        helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailConditions", is(policeBailConditions)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description", is("Remanded into Custody"))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantToYouth() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }

    @Test
    public void shouldUpdateDefendantDetailsWithCustodyEstablishment() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendantWithCustody();

        // then
        helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }

    @Test
    public void shouldUpdateExactlyMatchedOtherDefendantsDetails_WithCustodyEstablishment_WhenMultipleCasesAreRelatedToDefendant() throws Exception {
        // initiation of first case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            prosecutionCaseId_1, hearingId, defendantId_1, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(prosecutionCaseId_1, defendantId_1, getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1));
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);


        // initiation of second case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }

        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        try (final MessageConsumer publicEventConsumerForDefendantUpdated = publicEvents
                .createPublicConsumer("public.progression.case-defendant-changed")){
            matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
            prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList());
            pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
            prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
            pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);
            verifyInMessagingQueueForDefendantUpdated(publicEventConsumerForDefendantUpdated);
        }
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(randomUUID().toString())
                        .build());

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s/lsm-info", prosecutionCaseId_2)), PROGRESSION_QUERY_CASE_LSM_INFO).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(anyOf(allOf(withJsonPath("$.matchedDefendantCases[0].caseId", equalTo(prosecutionCaseId_2)),
                                withJsonPath("$.matchedDefendantCases[0].defendants[0].firstName", equalTo("Harry")),
                                withJsonPath("$.matchedDefendantCases[0].defendants[0].middleName", equalTo("Jack")),
                                withJsonPath("$.matchedDefendantCases[0].defendants[0].lastName", equalTo("Kane Junior")),
                                withJsonPath("$.matchedDefendantCases[0].defendants[0].id", equalTo(defendantId_2)),
                                withJsonPath("$.matchedDefendantCases[0].defendants[0].masterDefendantId", equalTo(masterDefendantId_1)),
                                withJsonPath("$.matchedDefendantCases[0].defendants[0].offences[0].offenceTitle", equalTo("ROBBERY"))),

                                allOf(withJsonPath("$.matchedDefendantCases[1].caseId", equalTo(prosecutionCaseId_2)),
                                        withJsonPath("$.matchedDefendantCases[1].defendants[0].firstName", equalTo("Harry")),
                                        withJsonPath("$.matchedDefendantCases[1].defendants[0].middleName", equalTo("Jack")),
                                        withJsonPath("$.matchedDefendantCases[1].defendants[0].lastName", equalTo("Kane Junior")),
                                        withJsonPath("$.matchedDefendantCases[1].defendants[0].id", equalTo(defendantId_2)),
                                        withJsonPath("$.matchedDefendantCases[1].defendants[0].masterDefendantId", equalTo(masterDefendantId_1)),
                                        withJsonPath("$.matchedDefendantCases[1].defendants[0].offences[0].offenceTitle", equalTo("ROBBERY")))
                        ))
                );
        helper.updateDefendantWithCustodyEstablishmentInfo(prosecutionCaseId_1 , defendantId_1, masterDefendantId_1);

        // then
        //helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, defendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, custodyEstablishmentDefendantUpdatedMatchers);
    }


    @Test
    public void shouldUpdateMatchedOtherDefendantsDetails_WithCustodyEstablishment_WhenThreeCasesAreRelatedToDefendant() throws Exception {
        final String matchedCaseId_1 = randomUUID().toString();
        final String matchedDefendant_1 = randomUUID().toString();
        final String matchedCaseId_2 = randomUUID().toString();
        final String matchedDefendant_2 = randomUUID().toString();
        final String matchedCaseId_3 = randomUUID().toString();
        final String matchedDefendant_3 = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();
        // initiation of first case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(matchedCaseId_1, matchedDefendant_1, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_1, hearingId, matchedDefendant_1, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1, emptyList());
        pollProsecutionCasesProgressionFor(matchedCaseId_1, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(matchedCaseId_1, matchedDefendant_1, getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1));
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        // initiation of second case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(matchedCaseId_2, matchedDefendant_2, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_2, hearingId, matchedDefendant_2, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_2, matchedDefendant_2, emptyList());
        pollProsecutionCasesProgressionFor(matchedCaseId_2, prosecutionCaseMatchers);


        // initiation of third case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(matchedCaseId_3, matchedDefendant_3, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_3, hearingId, matchedDefendant_3, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_3, matchedDefendant_3, emptyList());
        pollProsecutionCasesProgressionFor(matchedCaseId_3, prosecutionCaseMatchers);


        // match defendant2 associated to case 2
        try (final MessageConsumer publicEventConsumerForDefendantUpdated = publicEvents
                .createPublicConsumer("public.progression.case-defendant-changed")) {
            matchDefendant(matchedCaseId_2, matchedDefendant_2, matchedCaseId_1, matchedDefendant_1, masterDefendantId);

            prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1, emptyList());
            pollProsecutionCasesProgressionFor(matchedCaseId_1, prosecutionCaseMatchers);

            prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_2, matchedDefendant_2, emptyList());
            pollProsecutionCasesProgressionFor(matchedCaseId_2, prosecutionCaseMatchers);
            verifyInMessagingQueueForDefendantUpdated(publicEventConsumerForDefendantUpdated);
        }

        // match defendant3 associated to case 3
        try (final MessageConsumer publicEventConsumerForDefendantUpdated = publicEvents
                .createPublicConsumer("public.progression.case-defendant-changed")) {
            matchDefendant(matchedCaseId_3, matchedDefendant_3, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
            prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1, emptyList());
            pollProsecutionCasesProgressionFor(matchedCaseId_1, prosecutionCaseMatchers);

            prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_3, matchedDefendant_3, emptyList());
            pollProsecutionCasesProgressionFor(matchedCaseId_3, prosecutionCaseMatchers);
            verifyInMessagingQueueForDefendantUpdated(publicEventConsumerForDefendantUpdated);
        }

        helper.updateDefendantWithCustodyEstablishmentInfo(matchedCaseId_1 , matchedDefendant_1, masterDefendantId);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollProsecutionCasesProgressionFor(matchedCaseId_1, defendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(matchedCaseId_2, custodyEstablishmentDefendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(matchedCaseId_3, custodyEstablishmentDefendantUpdatedMatchers);
    }

    @Test
    public void shouldUpdateMatchedOtherDefendantsDetails_WithNonEmptyCustodyEstablishment_WithEmptyCustodyEstablishment() throws Exception {
        final String matchedCaseId_1 = randomUUID().toString();
        final String matchedDefendant_1 = randomUUID().toString();
        final String matchedCaseId_2 = randomUUID().toString();
        final String matchedDefendant_2 = randomUUID().toString();
        final String matchedCaseId_3 = randomUUID().toString();
        final String matchedDefendant_3 = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();
        // initiation of first case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(matchedCaseId_1, matchedDefendant_1, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_1, hearingId, matchedDefendant_1, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1, emptyList());
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(matchedCaseId_1, matchedDefendant_1, getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1));
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        // initiation of second case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(matchedCaseId_2, matchedDefendant_2, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_2, hearingId, matchedDefendant_2, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            pollProsecutionCasesProgressionFor(matchedCaseId_1, prosecutionCaseMatchers);
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }

        prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_2, matchedDefendant_2, emptyList());
        // initiation of third case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(matchedCaseId_3, matchedDefendant_3, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_3, hearingId, matchedDefendant_3, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());

            pollProsecutionCasesProgressionFor(matchedCaseId_2, prosecutionCaseMatchers);
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        prosecutionCaseMatchers = getProsecutionCaseMatchers(matchedCaseId_3, matchedDefendant_3, emptyList());

        // match defendant2 associated to case 2
        try (final MessageConsumer publicEventConsumerForDefendantUpdated = publicEvents
                .createPublicConsumer("public.progression.case-defendant-changed")) {
            matchDefendant(matchedCaseId_2, matchedDefendant_2, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                            matchedCaseId_2, hearingId, matchedDefendant_2, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                            .withUserId(randomUUID().toString())
                            .build());
            pollProsecutionCasesProgressionFor(matchedCaseId_3, prosecutionCaseMatchers);
            verifyInMessagingQueueForDefendantUpdated(publicEventConsumerForDefendantUpdated);
        }

        // match defendant3 associated to case 3
        try (final MessageConsumer publicEventConsumerForDefendantUpdated = publicEvents
                .createPublicConsumer("public.progression.case-defendant-changed")) {
            matchDefendant(matchedCaseId_3, matchedDefendant_3, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
            verifyInMessagingQueueForDefendantUpdated(publicEventConsumerForDefendantUpdated);
        }

        helper.updateDefendantWithCustodyEstablishmentInfo(matchedCaseId_1 , matchedDefendant_1, masterDefendantId);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollProsecutionCasesProgressionFor(matchedCaseId_1, defendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(matchedCaseId_2, custodyEstablishmentDefendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(matchedCaseId_3, custodyEstablishmentDefendantUpdatedMatchers);

        helper.updateDefendantWithEmptyCustodyEstablishmentInfo(matchedCaseId_1 , matchedDefendant_1, masterDefendantId);

        final Matcher[] defendantUpdatedMatchersEmptyCustodyEstablishment = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment = new Matcher[]{
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        pollProsecutionCasesProgressionFor(matchedCaseId_1, defendantUpdatedMatchersEmptyCustodyEstablishment);
        pollProsecutionCasesProgressionFor(matchedCaseId_2, custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment);
        pollProsecutionCasesProgressionFor(matchedCaseId_3, custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment);
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantWithYouthFlagSetToTrue() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        helper.updateYouthFlagForDefendant();

        helper.verifyInActiveMQ();
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].isYouth", is(true)));
        helper.verifyInMessagingQueueForDefendentChanged();
    }


    @Test
    public void shouldUpdateDefendantDetailsWithHearingLanguageNeeds() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)))));

        helper.updateDefendantWithHearingLanguageNeeds("ENGLISH");
        helper.verifyInActiveMQ();

        final Matcher[] matchers = {withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.hearingLanguageNeeds", is("ENGLISH"))};
        pollProsecutionCasesProgressionFor(caseId, matchers);
        helper.verifyInMessagingQueueForDefendentChanged();
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

    private void verifyInMessagingQueueForProsecutionCaseCreated(final MessageConsumer publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }

    private void verifyInMessagingQueueForDefendantUpdated(final MessageConsumer publicEventConsumerForDefendantUpdated) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForDefendantUpdated);
        assertTrue(message.isPresent());
    }
}