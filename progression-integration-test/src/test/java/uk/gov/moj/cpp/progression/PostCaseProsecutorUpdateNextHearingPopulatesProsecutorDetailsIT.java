package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingWithProsecutorInfo;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PostCaseProsecutorUpdateNextHearingPopulatesProsecutorDetailsIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private String caseId;
    private String defendantId;

    private CaseProsecutorUpdateHelper caseProsecutorUpdateHelper;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);
        setupLoggedInUsersPermissionQueryStub();
        stubDocumentCreate(DOCUMENT_TEXT);
        stubInitiateHearing();
    }

    @Test
    public void shouldUpdateCaseProsecutorAndNextHearingHasUpdatedProsecutorDetails() throws Exception {
        // Create Prosecution Case
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        // Update prosecutor details
        caseProsecutorUpdateHelper.updateCaseProsecutor();

        // verify updated prosecutor details
        caseProsecutorUpdateHelper.verifyInActiveMQ();
        caseProsecutorUpdateHelper.verifyInMessagingQueueForProsecutorUpdated(0);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        //Create an ad-hock Hearing
        try (final MessageConsumer messageConsumerListHearingRequested = privateEvents
                .createPrivateConsumer("progression.event.list-hearing-requested")) {

            // Ad-hock hearing
            PreAndPostConditionHelper.listNewHearing(caseId, defendantId);
            final JsonPath message = retrieveMessage(messageConsumerListHearingRequested, isJson(allOf(
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(caseId)),
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(defendantId)),
                    withJsonPath("$.listNewHearing.bookingType", is("Video")),
                    withJsonPath("$.listNewHearing.priority", is("High")),
                    withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                    withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL"))
            )));
            assertNotNull(message);
        }
        //Verify hearing with prosecutors details populated into it.
        verifyPostListCourtHearingWithProsecutorInfo(caseId, defendantId, "8e837de0-743a-4a2c-9db3-b2e678c48729");
    }
}

