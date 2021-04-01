package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"squid:S1607", "squid:S2925"})
public class UpdateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String COURT_APPLICATION_UPDATED = "progression.event.court-application-proceedings-edited";

    private MessageConsumer consumerForCourtApplicationCreated;
    private MessageConsumer consumerForCourtApplicationUpdated;

    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
        consumerForCourtApplicationUpdated = privateEvents.createConsumer(COURT_APPLICATION_UPDATED);
    }

    @After
    public void tearDown() throws JMSException {
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

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(applicationId);

        Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(applicationId, applicationMatchers);

        updateCourtApplication(applicationId, applicantId, caseId, defendantId, "","progression.command.update-court-application.json");

        verifyInMessagingQueueForCourtApplicationUpdated();

        Matcher[] updatedApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("b"))
        };

        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, updatedApplicationMatchers);

    }

    @SafeVarargs
    private final void verifyInitiateCourtProceedingsViewStoreUpdated(final String applicationId, final Matcher<? super ReadContext>... matchers) {
        poll(requestParams(getReadUrl("/court-proceedings/application/" + applicationId),
                "application/vnd.progression.query.court-proceedings-for-application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(allOf(matchers)));

    }

    private void verifyInMessagingQueueForCourtApplicationCreated(String applicationId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private void verifyInMessagingQueueForCourtApplicationUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationUpdated);
        assertTrue(message.isPresent());
        String outOfTimeReasons = message.get().getJsonObject("courtApplication").getString("outOfTimeReasons");
        assertThat(outOfTimeReasons, equalTo("b"));
    }
}

