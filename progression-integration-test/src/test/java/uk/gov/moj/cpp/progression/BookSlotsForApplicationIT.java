package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class BookSlotsForApplicationIT extends AbstractIT {

    private String applicationId;
    private String caseId;
    private String defendantId;

    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";

    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();

    @BeforeEach
    public void setUp() {
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldCreateAndListHearingAndBookSlotsForApplication() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        // Create application for the case
        //addCourtApplication(caseId, applicationId, "progression.command.create-court-application.json");
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(applicationId);

        final Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(applicationId, firstApplicationMatchers);

        verifyPostListCourtHearing(applicationId);
    }


    private static void verifyInMessagingQueueForCourtApplicationCreated(final String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

}

