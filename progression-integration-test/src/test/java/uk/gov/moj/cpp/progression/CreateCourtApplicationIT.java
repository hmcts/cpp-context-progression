package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import javax.jms.MessageConsumer;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("squid:S1607")
public class CreateCourtApplicationIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String COURT_APPLICATION_REJECTED = "public.progression.court-application-rejected";

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final MessageConsumer consumerForCourtApplicationRejected = publicEvents.createConsumer(COURT_APPLICATION_REJECTED);

    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        createMockEndpoints();
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldCreateStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String firstApplicationId = UUID.randomUUID().toString();
        addStandaloneCourtApplication(firstApplicationId,  UUID.randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        String secondApplicationId = UUID.randomUUID().toString();
        addStandaloneCourtApplication(secondApplicationId,  firstApplicationId, new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        String applicationResponse = getApplicationFor(firstApplicationId);
        JsonObject applicationJson = getJsonObject(applicationResponse);
        JsonObject courtApplication = applicationJson.getJsonObject("courtApplication");

        assertThat(courtApplication.getString("id"), equalTo(firstApplicationId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(courtApplication.getString("outOfTimeReasons"), equalTo("a"));
        assertThat(courtApplication.getString("applicationReference"), notNullValue(String.class));

        JsonObject linkedApplicationsSummary = applicationJson.getJsonArray("linkedApplicationsSummary").getJsonObject(0);
        assertThat(linkedApplicationsSummary.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(linkedApplicationsSummary.getString("applicationTitle"), equalTo("a"));
        assertThat(linkedApplicationsSummary.getString("applicationReference"), notNullValue(String.class));
        assertThat(linkedApplicationsSummary.getString("applicantDisplayName"), notNullValue(String.class));
        assertThat(linkedApplicationsSummary.getJsonArray("respondentDisplayNames"), notNullValue(JsonArray.class));
    }

    @Test
    public void shouldCreateCourtApplicationLinkedWithCaseAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        // Creating first application for the case
        String firstApplicationId = UUID.randomUUID().toString();
        addCourtApplication(caseId, firstApplicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");

        String applicationResponse = getApplicationFor(firstApplicationId);
        JsonObject applicationJson = getJsonObject(applicationResponse);
        JsonObject courtApplication = applicationJson.getJsonObject("courtApplication");

        assertThat(courtApplication.getString("id"), equalTo(firstApplicationId));
        assertThat(courtApplication.getString("linkedCaseId"), equalTo(caseId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(courtApplication.getString("outOfTimeReasons"), equalTo("a"));
        assertThat(courtApplication.getString("applicationReference"), equalTo(reference + "-1"));

        // Creating second application for the case
        String secondApplicationId = UUID.randomUUID().toString();
        addCourtApplication(caseId, secondApplicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-2");

        applicationResponse = getApplicationFor(secondApplicationId);
        applicationJson = getJsonObject(applicationResponse);
        courtApplication = applicationJson.getJsonObject("courtApplication");

        assertThat(courtApplication.getString("id"), equalTo(secondApplicationId));
        assertThat(courtApplication.getString("linkedCaseId"), equalTo(caseId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(courtApplication.getString("outOfTimeReasons"), equalTo("a"));
        assertThat(courtApplication.getString("applicationReference"), equalTo(reference + "-2"));

        String caseResponse = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutionCaseJson = getJsonObject(caseResponse);
        JsonArray summaries = prosecutionCaseJson.getJsonArray("linkedApplicationsSummary");

        assertThat(summaries.size(), equalTo(2));

        JsonObject summary = summaries.getJsonObject(0);

        assertThat(summary.getString("applicationTitle"), equalTo("a"));
        assertThat(summary.getBoolean("isAppeal"), equalTo(false));
        assertThat(summary.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(summary.getJsonArray("respondentDisplayNames").size(), equalTo(1));
    }

    @Test
    public void shouldRejectCourtApplicationWhenApplicantIsRespondent() throws Exception {
        // when
        addCourtApplication(caseId, UUID.randomUUID().toString(), "progression.command.create-court-application-reject.json");
        // then
        verifyInMessagingQueueForCourtApplicationRejected();
    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String arn) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertThat(arnResponse, equalTo(arn));
    }

    private static void verifyInMessagingQueueForCourtApplicationRejected() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationRejected);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertTrue(arnResponse.length() == 10);
    }

}

