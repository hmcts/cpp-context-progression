package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.*;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1607","squid:S2925"})
public class UpdateCourtApplicationIT {
    static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    static final String COURT_APPLICATION_UPDATED = "public.progression.court-application-updated";

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final MessageConsumer consumerForCourtApplicationUpdated = publicEvents.createConsumer(COURT_APPLICATION_UPDATED);

    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        createMockEndpoints();
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        consumerForCourtApplicationCreated.close();
        consumerForCourtApplicationUpdated.close();
    }

    @Test
    public void shouldUpdateCourtApplicationAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        // assert defendant witness statement
        assertThat(prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getString("witnessStatement"), equalTo("he did not do it"));
        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        String applicationId = UUID.randomUUID().toString();
        String applicantId = UUID.fromString("88cdf36e-93e4-41b0-8277-17d9dba7f06f").toString();

        addCourtApplication(caseId, applicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");

        String caseResponse = getApplicationFor(applicationId);
        JsonObject applicationJson = getJsonObject(caseResponse);
        JsonObject courtApplication = applicationJson.getJsonObject("courtApplication");

        assertThat(courtApplication.getString("id"), equalTo(applicationId));
        assertThat(courtApplication.getString("linkedCaseId"), equalTo(caseId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(courtApplication.getString("outOfTimeReasons"), equalTo("a"));
        assertThat(courtApplication.getString("applicationReference"), equalTo(reference + "-1"));

        updateCourtApplication(applicationId, applicantId, caseId, defendantId, "progression.command.update-court-application.json" );

        verifyInMessagingQueueForCourtApplicationUpdated();

        caseResponse = getApplicationFor(applicationId);
        applicationJson = getJsonObject(caseResponse);
        courtApplication = applicationJson.getJsonObject("courtApplication");

        assertThat(courtApplication.getString("id"), equalTo(applicationId));
        assertThat(courtApplication.getString("applicationStatus"), equalTo("DRAFT"));
        assertThat(courtApplication.getString("outOfTimeReasons"), equalTo("b"));

        JsonObject applicant = courtApplication.getJsonObject("applicant");
        assertThat(applicant.getString("synonym"), equalTo("test"));

        response = getProsecutioncasesProgressionFor(caseId);
        prosecutionCasesJsonObject = getJsonObject(response);
        // assert defendant witness statement
        //updated defendant in application will update defendant for prosecutionCase also
        assertThat(prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getString("witnessStatement"), equalTo("updated statement, he did it"));
    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String arn) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertThat(arnResponse, equalTo(arn));
    }

    private static void verifyInMessagingQueueForCourtApplicationUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationUpdated);
        assertTrue(message.isPresent());
        String outOfTimeReasons = message.get().getJsonObject("courtApplication").getString("outOfTimeReasons");
        assertThat(outOfTimeReasons, equalTo("b"));
    }
}

