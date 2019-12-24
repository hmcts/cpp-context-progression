package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"squid:S1607", "squid:S2925"})
public class UpdateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String COURT_APPLICATION_UPDATED = "public.progression.court-application-updated";

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final MessageConsumer consumerForCourtApplicationUpdated = publicEvents.createConsumer(COURT_APPLICATION_UPDATED);

    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
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

        final Matcher[] initialMatchers = getProsecutionCaseMatchers(caseId, defendantId, singletonList(withJsonPath("$.prosecutionCase.defendants[0].witnessStatement", is("he did not do it"))));
        String response = pollProsecutionCasesProgressionFor(caseId, initialMatchers);
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        // assert defendant witness statement
        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        String applicationId = randomUUID().toString();
        String applicantId = UUID.fromString("88cdf36e-93e4-41b0-8277-17d9dba7f06f").toString();

        addCourtApplication(caseId, applicationId, "progression.command.create-court-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");

        Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.linkedCaseId", is(caseId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("a")),
                withJsonPath("$.courtApplication.applicationReference", is(reference + "-1")),
        };

        pollForApplication(applicationId, applicationMatchers);

        updateCourtApplication(applicationId, applicantId, caseId, defendantId, "progression.command.update-court-application.json");

        verifyInMessagingQueueForCourtApplicationUpdated();

        Matcher[] updatedApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("b")),
                withJsonPath("$.courtApplication.applicant.synonym", is("test")),
        };

        pollForApplication(applicationId, updatedApplicationMatchers);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].witnessStatement", is("updated statement, he did it")));
    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String arn) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String arnResponse = message.get().getString("arn");
        assertThat(arnResponse, equalTo(arn));
    }

    private static void verifyInMessagingQueueForCourtApplicationUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationUpdated);
        assertTrue(message.isPresent());
        String outOfTimeReasons = message.get().getJsonObject("courtApplication").getString("outOfTimeReasons");
        assertThat(outOfTimeReasons, equalTo("b"));
    }
}

