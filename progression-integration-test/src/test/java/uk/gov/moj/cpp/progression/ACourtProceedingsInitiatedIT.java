package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForPartialOrExactMatchDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollPartialMatchDefendantFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatch;
import static uk.gov.moj.cpp.progression.util.PartialMatchDefendantHelper.getPartialMatchDefendantMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ACourtProceedingsInitiatedIT extends AbstractIT {

    private static final JmsMessageConsumerClient publicEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
    }

    @Test
    public void shouldInitiateCourtProceedingsWithDefendantAsYouth() throws IOException {
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();

        final Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);

        verifyPostListCourtHearing(caseId, defendantId, true);
    }

    @Test
    public void shouldInitiateCourtProceedingWithPartialMatchDefendant() throws IOException, JSONException {
        final String matchedCaseId_1 = randomUUID().toString();
        final String matchedDefendant_1 = randomUUID().toString();
        final String matchedCaseId_2 = randomUUID().toString();
        final String matchedDefendant_2 = randomUUID().toString();
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";

        initiateCourtProceedingsWithoutCourtDocument(matchedCaseId_1, matchedDefendant_1, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(matchedCaseId_1, getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1));

        initiateCourtProceedingsWithoutCourtDocument(matchedCaseId_2, matchedDefendant_2, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(matchedCaseId_2, getProsecutionCaseMatchers(matchedCaseId_2, matchedDefendant_2));

        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatch(matchedCaseId_1, matchedCaseId_2, matchedDefendant_1, matchedDefendant_2, pncId, croNumber);
        String caseReceivedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(ZonedDateTime.now());

        initiateCourtProceedingsForPartialOrExactMatchDefendants(caseId, defendantId, caseReceivedDate);

        final Matcher[] partialMatchDefendantMatchers = getPartialMatchDefendantMatchers(caseId, defendantId, pncId, croNumber);
        pollPartialMatchDefendantFor(partialMatchDefendantMatchers);
    }

    @Test
    public void shouldInitiateCourtProceedingWithExactMatchDefendant() throws IOException, JSONException {

        final String matchedCaseId_1 = randomUUID().toString();
        final String matchedDefendant_1 = randomUUID().toString();
        final String matchedCaseId_2 = randomUUID().toString();
        final String matchedDefendant_2 = randomUUID().toString();
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";

        initiateCourtProceedingsWithoutCourtDocument(matchedCaseId_1, matchedDefendant_1, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(matchedCaseId_1, getProsecutionCaseMatchers(matchedCaseId_1, matchedDefendant_1));

        initiateCourtProceedingsWithoutCourtDocument(matchedCaseId_2, matchedDefendant_2, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(matchedCaseId_2, getProsecutionCaseMatchers(matchedCaseId_2, matchedDefendant_2));

        stubUnifiedSearchQueryExactMatchWithResults(matchedCaseId_1, matchedCaseId_2, matchedDefendant_1, matchedDefendant_2, pncId, croNumber);
        stubUnifiedSearchQueryPartialMatch(matchedCaseId_1, matchedCaseId_2, matchedDefendant_1, matchedDefendant_2, pncId, croNumber);
        String caseReceivedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(ZonedDateTime.now());

        initiateCourtProceedingsForPartialOrExactMatchDefendants(caseId, defendantId, caseReceivedDate);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is("0a5372c5-b60f-4d95-8390-8c6462e2d7af")))));
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumer);
        assertTrue(message.isPresent());
    }
}


