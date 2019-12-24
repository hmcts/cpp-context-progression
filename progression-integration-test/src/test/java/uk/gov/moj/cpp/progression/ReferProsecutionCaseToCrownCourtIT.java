package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithMinimumAttributes;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryEthinicityData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getCourtDocumentMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class ReferProsecutionCaseToCrownCourtIT extends AbstractIT {
    private static final String REFER_PROSECUTION_CASES_TO_COURT_REJECTED = "public.progression.refer-prosecution-cases-to-court-rejected";

    private static final MessageConsumer consumerForReferToCourtRejected = publicEvents.createConsumer(REFER_PROSECUTION_CASES_TO_COURT_REJECTED);

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
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);

        List<Matcher> additionalMatchers = newArrayList(
                withJsonPath("$.courtDocuments[0].materials", hasSize(2)),
                withJsonPath("$.courtDocuments[0].containsFinancialMeans", is(true))
        );
        additionalMatchers.addAll(getCourtDocumentMatchers(caseId, courtDocumentId, materialIdActive, 0));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, additionalMatchers));
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
        final Matcher[] prosecutionCaseMatchersWithNoDocuments = getProsecutionCaseMatchers(caseId, defendantId, singletonList(withJsonPath("$.courtDocuments", empty())));
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

        List<Matcher> additionalMatchers = newArrayList(
                withJsonPath("$.courtDocuments[0].materials", hasSize(2))
        );
        additionalMatchers.addAll(getCourtDocumentMatchers(caseId, courtDocumentId, materialIdActive, 0));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, additionalMatchers));

        //Remove document
        addRemoveCourtDocument(courtDocumentId, materialIdActive, true);

        //read document
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.courtDocuments", empty()));

        //undo remove
        addRemoveCourtDocument(courtDocumentId, materialIdActive, false);
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.courtDocuments", hasSize(1)));
    }


    @Test
    public void shouldGetProsecutionCaseAtAGlance() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, newArrayList(
                withJsonPath("$.courtDocuments", empty()),
                withJsonPath("$.caseAtAGlance.id", is(caseId))
        )));
    }


    private static void verifyInMessagingQueueForReferToCourtsRejcted() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForReferToCourtRejected);
        assertTrue(message.isPresent());
    }


}

