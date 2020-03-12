package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class CreateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String COURT_APPLICATION_REJECTED = "public.progression.court-application-rejected";

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final MessageConsumer consumerForCourtApplicationRejected = publicEvents.createConsumer(COURT_APPLICATION_REJECTED);

    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldCreateStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String firstApplicationId = randomUUID().toString();
        addStandaloneCourtApplication(firstApplicationId, randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        String secondApplicationId = randomUUID().toString();
        addStandaloneCourtApplication(secondApplicationId, firstApplicationId, new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("a")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue(String.class)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("DRAFT")),
                withJsonPath("$.linkedApplicationsSummary[0].applicationTitle", is("a")),
                withJsonPath("$.linkedApplicationsSummary[0].applicationReference", notNullValue(String.class)),
                withJsonPath("$.linkedApplicationsSummary[0].applicantDisplayName", notNullValue(String.class)),
                withJsonPath("$.linkedApplicationsSummary[0].respondentDisplayNames", notNullValue(JsonArray.class))
        };

        pollForApplication(firstApplicationId, matchers);
    }

    @Test
    public void shouldCreateCourtApplicationLinkedWithCaseAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        addCourtApplication(caseId, firstApplicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");

        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.linkedCaseId", is(caseId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("a")),
                withJsonPath("$.courtApplication.applicationReference", is(reference + "-1")),
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

        // Creating second application for the case
        String secondApplicationId = randomUUID().toString();
        addCourtApplication(caseId, secondApplicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-2");

        Matcher[] secondApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(secondApplicationId)),
                withJsonPath("$.courtApplication.linkedCaseId", is(caseId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("a")),
                withJsonPath("$.courtApplication.applicationReference", is(reference + "-2")),
        };

        pollForApplication(secondApplicationId, secondApplicationMatchers);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(2)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationTitle", is("Application for bad character")),
                withJsonPath("$.linkedApplicationsSummary[0].isAppeal", is(false)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("DRAFT")),
                withJsonPath("$.linkedApplicationsSummary[0].respondentDisplayNames", hasSize(1)),
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    @Test
    public void shouldRejectCourtApplicationWhenApplicantIsRespondent() throws Exception {
        // when
        addCourtApplication(caseId, randomUUID().toString(), "progression.command.create-court-application-reject.json");
        // then
        verifyInMessagingQueueForCourtApplicationRejected();
    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String arn) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String arnResponse = message.get().getString("arn");
        assertThat(arnResponse, equalTo(arn));
    }

    private static void verifyInMessagingQueueForCourtApplicationRejected() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationRejected);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String arnResponse = message.get().getString("arn");
        assertThat(10, is(arnResponse.length()));
    }

}

