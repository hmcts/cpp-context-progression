package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StandaloneApplicationIT extends AbstractIT {

    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private MessageConsumer consumerForCourtApplicationCreated;

    @Before
    public void setUp() {
        consumerForCourtApplicationCreated = privateEvents.createConsumer(COURT_APPLICATION_CREATED_PRIVATE_EVENT);
    }

    @After
    public void tearDown() throws Exception {
        consumerForCourtApplicationCreated.close();
    }

    @Test
    public void shouldInitiateCourtProceedingsForCourtHearing() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-2.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsForBoxHearing() throws Exception {
        stubInitiateHearing();
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, UUID.randomUUID().toString(),"applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "IN_PROGRESS");
        pollForCourtApplication(applicationId, applicationMatchers);
    }

    private Matcher[] createMatchersForAssertion(final String applicationId, final String applicationStatus) {
        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.applicationStatus", is(applicationStatus)),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for standalone test"))
        };

        return applicationMatchers;
    }

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(10, is(applicationReference.length()));
    }
}
