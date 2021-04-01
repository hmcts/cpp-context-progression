package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyHearingApplicationLinkCreated;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyHearingInMessagingQueueForReferToCourt;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyPublicEventForHearingExtended;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReferApplicationToCourtIT extends AbstractIT {

    private String applicationId;
    private String caseId_1;
    private String defendantId_1;
    private String caseId_2;
    private String defendantId_2;
    private String caseId;
    private String defendantId;
    private MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged;

    @Before
    public void setUp() {
        caseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        caseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
        stubInitiateHearing();
    }

    @After
    public void tearDown() throws Exception {
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
    }

    @Test
    public void shouldReferApplicationToExistingHearing() throws Exception {
        addProsecutionCaseToCrownCourt(caseId_1, defendantId_1);
        pollProsecutionCasesProgressionFor(caseId_1, getProsecutionCaseMatchers(caseId_1, defendantId_1));
        addProsecutionCaseToCrownCourt(caseId_2, defendantId_2);
        pollProsecutionCasesProgressionFor(caseId_2, getProsecutionCaseMatchers(caseId_2, defendantId_2));

        String hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId_1, caseId_2, "applications/progression.initiate-court-proceedings-for-refer-to-court.json", hearingId);
        verifyHearingInMessagingQueueForReferToCourt();
        verifyPublicEventForHearingExtended(hearingId);
        verifyHearingApplicationLinkCreated(hearingId);
    }

    @Test
    public void shouldListCourtHearing() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        // Create application for the case
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        final Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue(String.class)),
        };

        pollForApplication(applicationId, firstApplicationMatchers);

        verifyPostListCourtHearing(applicationId);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(1)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("UN_ALLOCATED"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }
}
