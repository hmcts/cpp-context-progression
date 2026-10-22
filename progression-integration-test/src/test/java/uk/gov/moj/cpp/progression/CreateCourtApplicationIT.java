package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
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
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.removeHearingTypePermission;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubHearingTypePermission;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class CreateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String MH_ACTIVE_CASE_FIXTURE =
            "applications/progression.initiate-court-proceedings-mh-source-active-case.json";
    private static final String MH_INACTIVE_CASE_FIXTURE =
            "applications/progression.initiate-court-proceedings-mh-source-inactive-case.json";

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

    @Test
    public void shouldNotStoreOffencesWhenApplicationSourceIsMHAndCaseIsActive() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, MH_ACTIVE_CASE_FIXTURE);

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

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, MH_INACTIVE_CASE_FIXTURE);

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
        try {
        stubHearingTypePermission(standaloneApplicationTypeId, randomUUID().toString());

        Response response = initiateCourtProceedingsForCourtApplication(randomUUID().toString(),
                "applications/progression.initiate-court-proceedings-for-standalone-application.json");

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));

        stubHearingTypePermission(standaloneApplicationTypeId, "8cdfd3da-8900-42ca-9835-9f29d1e03cd6");
        response = initiateCourtProceedingsForCourtApplication(randomUUID().toString(),
                "applications/progression.initiate-court-proceedings-for-standalone-application.json");

        assertThat(response.getStatusCode(), is(SC_ACCEPTED));
        } finally {
            removeHearingTypePermission(standaloneApplicationTypeId);
        }
    }

    private void verifyCourtApplicationCreatedEventPublished(final String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated, courtApplicationIdMatches(applicationId));
        assertTrue(message.isPresent(), "Expected court-application-created event on JMS topic for applicationId " + applicationId);
        final String idFromEvent = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idFromEvent, equalTo(applicationId));
    }

    private void verifyInMessagingQueueForCourtApplicationCreated(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated, courtApplicationIdMatches(applicationId));
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private static Matcher<JsonObject> courtApplicationIdMatches(final String applicationId) {
        return new org.hamcrest.BaseMatcher<>() {
            @Override
            public boolean matches(final Object item) {
                return item instanceof JsonObject
                        && ((JsonObject) item).containsKey("courtApplication")
                        && applicationId.equals(((JsonObject) item).getJsonObject("courtApplication").getString("id", null));
            }

            @Override
            public void describeTo(final org.hamcrest.Description description) {
                description.appendText("courtApplication.id = " + applicationId);
            }
        };
    }

    private void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String referenceResponse = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(11, is(referenceResponse.length()));
    }

}

