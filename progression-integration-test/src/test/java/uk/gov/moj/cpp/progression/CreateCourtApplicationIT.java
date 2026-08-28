package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubEmptyPermissionsQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubHearingTypePermission;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class CreateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String MH_ACTIVE_CASE_FIXTURE =
            "applications/progression.initiate-court-proceedings-mh-source-active-case.json";
    private static final String MH_INACTIVE_CASE_FIXTURE =
            "applications/progression.initiate-court-proceedings-mh-source-inactive-case.json";

    private final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(COURT_APPLICATION_CREATED)
            .getMessageConsumerClient();

    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubEmptyPermissionsQuery();
    }

    @AfterEach
    public void restoreDefaultPermissionsStub() {
        // Hearing-type Locked stubs must not leak into later ITs in the same JVM.
        stubEmptyPermissionsQuery();
    }

    @Test
    public void shouldCreateStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String firstApplicationId = randomUUID().toString();
        addStandaloneCourtApplication(firstApplicationId, randomUUID().toString(),
                new CourtApplicationsHelper.CourtApplicationRandomValues(),
                "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated(firstApplicationId);

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

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        String firstApplicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId,
                "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json"));

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);

        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

        verifyPostListCourtHearing(firstApplicationId);

        String secondApplicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(secondApplicationId, caseId,
                "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json"));

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

    @Test
    public void shouldNotStoreOffencesWhenApplicationSourceIsMHAndCaseIsActive() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String applicationId = randomUUID().toString();

        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, caseId, MH_ACTIVE_CASE_FIXTURE));

        verifyCourtApplicationCreatedEventPublished(applicationId);

        final Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", notNullValue()),
                hasNoJsonPath("$.courtApplication.courtApplicationCases[0].offences")
        };

        pollForApplication(applicationId, matchers);
    }

    @Test
    public void shouldPreserveOffencesWhenApplicationSourceIsMHAndCaseIsInactive() throws Exception {
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String applicationId = randomUUID().toString();

        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, caseId, MH_INACTIVE_CASE_FIXTURE));

        verifyCourtApplicationCreatedEventPublished(applicationId);

        final Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].caseStatus", is("INACTIVE")),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0]", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is("CA03012"))
        };

        pollForApplication(applicationId, matchers);
    }

    @Test
    public void shouldRejectStandaloneApplicationWhenHearingTypeIsNotAnAllowedHearingType() throws Exception {
        // The standalone fixture carries applicationType id e857c8ea-... and hearingType id 8cdfd3da-...
        final String standaloneApplicationTypeId = "e857c8ea-cd95-47d1-842f-2d618e77a9b5";

        // Allowed hearing type for this application type differs from the one in the fixture,
        // so the initiate-court-proceedings-for-application command must be rejected.
        stubHearingTypePermission(standaloneApplicationTypeId, randomUUID().toString());

        Response response = initiateCourtProceedingsForCourtApplication(randomUUID().toString(),
                "applications/progression.initiate-court-proceedings-for-standalone-application.json");

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));

        stubHearingTypePermission(standaloneApplicationTypeId, "8cdfd3da-8900-42ca-9835-9f29d1e03cd6");

        response = initiateCourtProceedingsForCourtApplication(randomUUID().toString(),
                "applications/progression.initiate-court-proceedings-for-standalone-application.json");

        assertThat(response.getStatusCode(), is(SC_ACCEPTED));
    }

    private void verifyCourtApplicationCreatedEventPublished(final String applicationId) {
        final JsonPath message = retrieveCourtApplicationCreatedMessage(applicationId);
        assertNotNull(message, "Expected court-application-created event for applicationId=" + applicationId);
        assertThat(message.getString("courtApplication.id"), equalTo(applicationId));
    }

    private void verifyInMessagingQueueForCourtApplicationCreated(final String applicationId) {
        final JsonPath message = retrieveCourtApplicationCreatedMessage(applicationId);
        assertNotNull(message, "Expected court-application-created event for applicationId=" + applicationId);
        assertThat(message.getString("courtApplication.id"), equalTo(applicationId));
    }

    private void verifyInMessagingQueueForStandaloneCourtApplicationCreated(final String applicationId) {
        final JsonPath message = retrieveCourtApplicationCreatedMessage(applicationId);
        assertNotNull(message, "Expected court-application-created event for applicationId=" + applicationId);
        final String referenceResponse = message.getString("courtApplication.applicationReference");
        assertThat(referenceResponse, notNullValue());
        assertThat(referenceResponse.length(), greaterThan(0));
    }

    /**
     * Drain until the create event for this application id is found — avoids asserting a leftover
     * message from another IT on the shared public topic.
     */
    private JsonPath retrieveCourtApplicationCreatedMessage(final String applicationId) {
        return retrieveMessageAsJsonPath(consumerForCourtApplicationCreated,
                isJson(allOf(withJsonPath("$.courtApplication.id", is(applicationId)))));
    }

}
