package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import com.jayway.restassured.path.json.JsonPath;
import java.io.IOException;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;

public class ListNewHearingIT extends AbstractIT{

    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    final String CASE_ID = randomUUID().toString();
    final String DEFENDANT_ID = randomUUID().toString();

    @Before
    public void setUp(){
        setupLoggedInUsersPermissionQueryStub();
    }

    @Test
    public void shouldCreateNewHearing() throws IOException, JMSException {

        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, generateUrn());
        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, DEFENDANT_ID));
        String hearingId;
        try (final MessageConsumer messageConsumerListHearingRequested = privateEvents
                .createPrivateConsumer("progression.event.list-hearing-requested")) {

            PreAndPostConditionHelper.listNewHearing(CASE_ID, DEFENDANT_ID);

            final JsonPath message = retrieveMessage(messageConsumerListHearingRequested, isJson(allOf(
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].prosecutionCaseId", is(CASE_ID)),
                    withJsonPath("$.listNewHearing.listDefendantRequests[0].defendantId", is(DEFENDANT_ID)),
                    withJsonPath("$.listNewHearing.bookingType", is("Video")),
                    withJsonPath("$.listNewHearing.priority", is("High")),
                    withJsonPath("$.listNewHearing.specialRequirements", hasSize(2)),
                    withJsonPath("$.listNewHearing.specialRequirements", hasItems("RSZ", "CELL"))
            )));
            assertNotNull(message);
            hearingId = message.getString("hearingId");

        }


        verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID, "8e837de0-743a-4a2c-9db3-b2e678c48729");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("MAGISTRATES"))
        );


    }
}
