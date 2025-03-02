package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.time.LocalDate;

import javax.json.JsonObject;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import org.hamcrest.Matcher;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListingSearchHearingsQuery;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


public class ProsecutionCaseUpdateDefendantIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PROGRESSION_QUERY_CASE_LSM_INFO = "application/vnd.progression.query.case-lsm-info+json";
    ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String defendantId;
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
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
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
        final JmsMessageConsumerClient publicEventsCaseDefendantChanged = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-defendant-changed").getMessageConsumerClient();
        helper.updateDefendantWithPoliceBailInfo(policeBailStatusId, policeBailStatusDesc, policeBailConditions);
        helper.verifyInMessagingQueueForDefendantChanged(publicEventsCaseDefendantChanged);

        // then
        Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailConditions", is(policeBailConditions)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description", is("Remanded into Custody"))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);

        // when
        helper.updateDefendantWithCustody();

        // then
        defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);

        helper.updateYouthFlagForDefendant();

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].isYouth", is(true)));

        helper.updateDefendantWithHearingLanguageNeeds("ENGLISH");

        final Matcher[] matchers = {withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.hearingLanguageNeeds", is("ENGLISH"))};
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    @Test
    public void shouldUpdateExactlyMatchedOtherDefendantsDetails_WithCustodyEstablishment_WhenMultipleCasesAreRelatedToDefendant() throws Exception {
        // initiation of first case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_1, hearingId, defendantId_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        hearingId = pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1);
        System.out.println("hearingId: " + hearingId);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        // initiation of second case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, randomUUID().toString(), materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", not(masterDefendantId_1))
        );

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId_1))
        );
        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForResponse(format("/prosecutioncases/%s/lsm-info", prosecutionCaseId_2),
                PROGRESSION_QUERY_CASE_LSM_INFO,
                randomUUID().toString(),
                anyOf(allOf(withJsonPath("$.matchedDefendantCases[0].caseId", equalTo(prosecutionCaseId_2)),
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
                ));
        helper.updateDefendantWithCustodyEstablishmentInfo(prosecutionCaseId_1, defendantId_1, masterDefendantId_1);

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
        initiateCourtProceedingsForMatchedDefendants(matchedCaseId_1, matchedDefendant_1, masterDefendantId);
        pollProsecutionCasesProgressionFor(matchedCaseId_1);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_1, hearingId, matchedDefendant_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        hearingId = pollCaseAndGetHearingForDefendant(matchedCaseId_1, matchedDefendant_1);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        // initiation of second case
        initiateCourtProceedingsForMatchedDefendants(matchedCaseId_2, matchedDefendant_2, randomUUID().toString());
        pollProsecutionCasesProgressionFor(matchedCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", not(masterDefendantId))
        );

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_2, hearingId, matchedDefendant_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);


        // initiation of third case
        initiateCourtProceedingsForMatchedDefendants(matchedCaseId_3, matchedDefendant_3, randomUUID().toString());
        pollProsecutionCasesProgressionFor(matchedCaseId_3,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", not(masterDefendantId))
        );

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_3, hearingId, matchedDefendant_3, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);


        // match defendant2 associated to case 2
        matchDefendant(matchedCaseId_2, matchedDefendant_2, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
        pollProsecutionCasesProgressionFor(matchedCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId))
        );

        // match defendant3 associated to case 3
        matchDefendant(matchedCaseId_3, matchedDefendant_3, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
        pollProsecutionCasesProgressionFor(matchedCaseId_3,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId))
        );

        helper.updateDefendantWithCustodyEstablishmentInfo(matchedCaseId_1, matchedDefendant_1, masterDefendantId);

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
        initiateCourtProceedingsForMatchedDefendants(matchedCaseId_1, matchedDefendant_1, masterDefendantId);
        pollProsecutionCasesProgressionFor(matchedCaseId_1);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_1, hearingId, matchedDefendant_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        hearingId = pollCaseAndGetHearingForDefendant(matchedCaseId_1, matchedDefendant_1);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        // initiation of second case
        initiateCourtProceedingsForMatchedDefendants(matchedCaseId_2, matchedDefendant_2, randomUUID().toString());
        pollProsecutionCasesProgressionFor(matchedCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", not(masterDefendantId))
        );

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_2, hearingId, matchedDefendant_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        // initiation of third case
        initiateCourtProceedingsForMatchedDefendants(matchedCaseId_3, matchedDefendant_3, randomUUID().toString());
        pollProsecutionCasesProgressionFor(matchedCaseId_3,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", not(masterDefendantId))
        );

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_3, hearingId, matchedDefendant_3, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        // match defendant2 associated to case 2
        matchDefendant(matchedCaseId_2, matchedDefendant_2, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
        pollProsecutionCasesProgressionFor(matchedCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId))
        );

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                matchedCaseId_2, hearingId, matchedDefendant_2, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        // match defendant3 associated to case 3
        matchDefendant(matchedCaseId_3, matchedDefendant_3, matchedCaseId_1, matchedDefendant_1, masterDefendantId);
        pollProsecutionCasesProgressionFor(matchedCaseId_3,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId))
        );

        helper.updateDefendantWithCustodyEstablishmentInfo(matchedCaseId_1, matchedDefendant_1, masterDefendantId);

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

        helper.updateDefendantWithEmptyCustodyEstablishmentInfo(matchedCaseId_1, matchedDefendant_1, masterDefendantId);

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


    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return new StringToJsonObjectConverter().convert(strPayload);
    }
}