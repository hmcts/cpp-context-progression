package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithMinimumAttributes;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithReportingRestrictions;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryEthinicityData;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithResults;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchersWithOffence;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ReferProsecutionCaseToCrownCourtIT extends AbstractIT {

    private static final JmsMessageConsumerClient consumerForReferToCourtRejected = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.refer-prosecution-cases-to-court-rejected").getMessageConsumerClient();

    private static final String SENT_FOR_LISTING_STATUS = "SENT_FOR_LISTING";

    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String reportingRestrictionId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        reportingRestrictionId = randomUUID().toString();
    }

    @AfterEach
    public void tearDown() {
        // one off change -- need to fix this properly
        stubQueryEthinicityData("/restResource/ref-data-ethnicities.json", randomUUID());
    }

    @Test
    public void shouldGetProsecutionCaseWithDocumentsAndGetConfirmation() throws Exception {

        final String matchedProsecutionCaseId_1 = randomUUID().toString();
        final String matchedDefendantId_1 = randomUUID().toString();
        final String matchedProsecutionCaseId_2 = randomUUID().toString();
        final String matchedDefendantId_2 = randomUUID().toString();
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";

        addProsecutionCaseToCrownCourt(matchedProsecutionCaseId_1, matchedDefendantId_1);
        Matcher[] matchers = getProsecutionCaseMatchers(matchedProsecutionCaseId_1, matchedDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(matchedProsecutionCaseId_1, matchers);

        addProsecutionCaseToCrownCourt(matchedProsecutionCaseId_2, matchedDefendantId_2);
        matchers = getProsecutionCaseMatchers(matchedProsecutionCaseId_2, matchedDefendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(matchedProsecutionCaseId_2, matchers);

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        stubUnifiedSearchQueryExactMatchWithResults(matchedProsecutionCaseId_1, matchedProsecutionCaseId_1, matchedDefendantId_1, matchedDefendantId_2, pncId, croNumber);

        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId);

        final Matcher<? super ReadContext>[] caseMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is("0a5372c5-b60f-4d95-8390-8c6462e2d7af"))));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, caseMatchers);

        List<Matcher> hearingMatchers = newArrayList(
                withJsonPath("$.hearingListingStatus", is(SENT_FOR_LISTING_STATUS)));

        getHearingForDefendant(hearingId, hearingMatchers.toArray(new Matcher[0]));

        final String expectedPayload = getPayload("expected/expected.progression.refer-case-to-court.json")
                .replace("COURT-DOCUMENT-ID1", courtDocumentId)
                .replace("CASE-ID", caseId);


        assertEquals(expectedPayload, getCourtDocumentsByCase(randomUUID().toString(), caseId), getCustomComparator());

    }

    @Test
    public void shouldGetProsecutionCaseWithDocumentsAndReferralRejected() throws Exception {
        stubQueryEthinicityData("/restResource/ref-data-ethnicities-with-noresults.json", randomUUID());
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId);
        // when
        verifyInMessagingQueueForReferToCourtsRejected();
    }

    @Test
    public void shouldGetProsecutionCaseWithoutDocuments() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final Matcher[] prosecutionCaseMatchersWithNoDocuments = getProsecutionCaseMatchersWithOffence(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchersWithNoDocuments);
    }

    @Test
    public void shouldGetProsecutionCaseWithMinimumMandatoryAttributes() throws Exception {
        addProsecutionCaseToCrownCourtWithMinimumAttributes(caseId, defendantId);
        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.initiationCode", is("J"))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    @Test
    public void shouldGetProsecutionCaseWithReportingRestrictions() throws Exception {
        addProsecutionCaseToCrownCourtWithReportingRestrictions(caseId, defendantId, reportingRestrictionId);
        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.initiationCode", is("J")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-11-11"))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    @Test
    public void shouldRemoveAndAddDocuments() throws Exception {
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, emptyList()));
        assertThat("Court Document Does not exist", getCourtDocumentsByCase(randomUUID().toString(), caseId).contains(caseId));

        final UUID supportUserGroup = randomUUID();
        setupAsAuthorisedUser(supportUserGroup, "stub-data/usersgroups.get-support-groups-by-user.json");

        //Remove document
        addRemoveCourtDocument(courtDocumentId, materialIdActive, true, supportUserGroup);

        //read document
        assertThat(getCourtDocumentsByCase(randomUUID().toString(), caseId).contains("{\"documentIndices\":[]}"), is(true));
    }


    private static void verifyInMessagingQueueForReferToCourtsRejected() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForReferToCourtRejected);
        assertThat(message.isPresent(), is(true));

    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[1].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[0].document.materials[0].id", (o1, o2) -> true),
                new Customization("documentIndices[0].document.materials[1].id", (o1, o2) -> true)
        );
    }
}

