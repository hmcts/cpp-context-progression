package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListingSearchHearingsQuery;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.time.LocalDate;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProsecutionCaseRelatedCasesIT extends AbstractIT {
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String prosecutionCaseId_1;
    private String prosecutionCaseId_2;
    private String masterDefendantId_1;
    private String defendantId_1_forMasterDefendantId_1;
    private String defendantId_2_forMasterDefendantId_1;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String courtCentreId;


    @BeforeEach
    public void setUp() {
        prosecutionCaseId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();

        masterDefendantId_1 = randomUUID().toString();

        defendantId_1_forMasterDefendantId_1 = randomUUID().toString();
        defendantId_2_forMasterDefendantId_1 = randomUUID().toString();

        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        courtCentreId = randomUUID().toString();
    }


    @Test
    public void shouldVerifyRelatedCasesWhenAllCasesInActive() throws Exception {
        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final String hearingId1 = pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId1);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_1, hearingId1, defendantId_1_forMasterDefendantId_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, defendantId_2_forMasterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final String hearingId2 = pollCaseAndGetHearingForDefendant(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId2);

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed.json",
                prosecutionCaseId_2, hearingId2, defendantId_2_forMasterDefendantId_1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);
        // match defendantId_2_forMasterDefendantId_1 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1);

        pollProsecutionCasesProgressionFor(prosecutionCaseId_1,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_2)),
                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY")),
                withJsonPath("$.relatedCases[0].cases[0].offences[0].maxPenalty", equalTo("Max Penalty"))
        );

        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_1)),
                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY"))
        );

        closeTheCase(prosecutionCaseId_1, masterDefendantId_1, hearingId1);
        closeTheCase(prosecutionCaseId_2, masterDefendantId_1, hearingId2);

        pollProsecutionCasesProgressionFor(prosecutionCaseId_1,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_2)),
                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY"))
        );

        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_1)),
                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY"))
        );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldVerifyRelatedCasesWhenCasesAreMix() throws IOException {
        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final String hearingId1 = pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1);

        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, defendantId_2_forMasterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);
        // match defendantId_2_forMasterDefendantId_1 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1);

        closeTheCase(prosecutionCaseId_1, masterDefendantId_1, hearingId1);

        pollProsecutionCasesProgressionFor(prosecutionCaseId_1,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")),
                withJsonPath("$.relatedCases[0]", is(anEmptyMap()))
        );

        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("ACTIVE")),
                withJsonPath("$.relatedCases[0]", is(anEmptyMap()))
        );
    }

    private void closeTheCase(final String caseId, final String defendantId, final String hearingId) {

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, randomUUID()), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                hearingId, defendantId, courtCentreId, "C", "Remanded into Custody", "2593cf09-ace0-4b7d-a746-0703a29f33b5"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventEnvelope);

    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
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
