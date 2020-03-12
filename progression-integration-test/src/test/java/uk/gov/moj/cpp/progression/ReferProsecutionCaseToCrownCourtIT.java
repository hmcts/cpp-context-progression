package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithMinimumAttributes;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryEthinicityData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@SuppressWarnings("squid:S1607")
public class ReferProsecutionCaseToCrownCourtIT extends AbstractIT {
    private static final String REFER_PROSECUTION_CASES_TO_COURT_REJECTED = "public.progression.refer-prosecution-cases-to-court-rejected";

    private static final MessageConsumer consumerForReferToCourtRejected = publicEvents.createConsumer(REFER_PROSECUTION_CASES_TO_COURT_REJECTED);

    public static final UUID SUPPORT_USER_GROUP = UUID.randomUUID();

    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referraReasonId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        referraReasonId = randomUUID().toString();
    }

    @After
    public void tearDown() {
        // one off change -- need to fix this properly
        stubQueryEthinicityData("/restResource/ref-data-ethnicities.json", randomUUID());
    }

    @Test
    public void shouldGetProsecutionCaseWithDocumentsAndGetConfirmation() throws Exception {
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, emptyList()));

        final String expectedPayload = getPayload("expected/expected.progression.refer-case-to-court.json")
                .replace("COURT-DOCUMENT-ID1", courtDocumentId)
                .replace("CASE-ID", caseId);


        assertEquals(expectedPayload, getCourtDocumentsByCase(UUID.randomUUID().toString(), caseId), getCustomComparator());

    }

    @Test
    public void shouldGetProsecutionCaseWithDocumentsAndReferralRejected() throws Exception {
        stubQueryEthinicityData("/restResource/ref-data-ethnicities-with-noresults.json", randomUUID());
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);
        // when
        verifyInMessagingQueueForReferToCourtsRejcted();
    }


    @Test
    public void shouldGetProsecutionCaseWithoutDocuments() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final Matcher[] prosecutionCaseMatchersWithNoDocuments = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
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
    public void shouldRemoveAndAddDocuments() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);


        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, emptyList()));

        assertThat("Court Document Does not exist", getCourtDocumentsByCase(UUID.randomUUID().toString(), caseId).contains(caseId));

        setupAsAuthorisedUser(SUPPORT_USER_GROUP, "stub-data/usersgroups.get-support-groups-by-user.json");

        //Remove document
        addRemoveCourtDocument(courtDocumentId, materialIdActive, true, SUPPORT_USER_GROUP);

        //read document
        assertThat(getCourtDocumentsByCase(UUID.randomUUID().toString(), caseId).contains("{\"documentIndices\":[]}"), is(true));

    }



    @Test
    public void shouldGetProsecutionCaseAtAGlance() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
    }


    private static void verifyInMessagingQueueForReferToCourtsRejcted() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForReferToCourtRejected);
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

