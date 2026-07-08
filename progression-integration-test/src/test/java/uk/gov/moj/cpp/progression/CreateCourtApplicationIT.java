package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class CreateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";

    private final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();

    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldCreateStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String firstApplicationId = randomUUID().toString();
        addStandaloneCourtApplication(firstApplicationId, randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("a")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue(String.class))
        };

        pollForApplication(firstApplicationId, matchers);
    }

    @Test
    public void shouldCreateCourtApplicationLinkedWithCaseAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);

        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

        verifyPostListCourtHearing(firstApplicationId);

        // Creating second application for the case
        String secondApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(secondApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(secondApplicationId);

        Matcher[] secondApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(secondApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(secondApplicationId, secondApplicationMatchers);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(2))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    private void verifyInMessagingQueueForCourtApplicationCreated(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String referenceResponse = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(11, is(referenceResponse.length()));
    }

}

