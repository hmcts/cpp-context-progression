package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplicationCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.SjpStub.setupSjpProsecutionCaseQueryStub;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class SummonsApplicationIT {

    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private static final String COURT_APPLICATION_UPDATED = "progression.event.court-application-proceedings-edited";

    private static final JmsMessageConsumerClient consumerForCourtApplicationUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(COURT_APPLICATION_UPDATED).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(COURT_APPLICATION_CREATED_PRIVATE_EVENT).getMessageConsumerClient();

    @Test
    public void shouldCreateLinkedApplicationWithSummons() throws Exception {
        final String applicationId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        setupSjpProsecutionCaseQueryStub(caseId, randomUUID().toString());
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.applicant.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for Summons application"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
        pollForCourtApplicationCase(caseId);
    }

    @Test
    public void shouldEditLinkedApplicationWithSummons() throws Exception {
        final String applicationId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        setupSjpProsecutionCaseQueryStub(caseId, randomUUID().toString());
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-summons-stand-alone-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchersForInitate = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.applicant.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for Summons application")),
                hasNoJsonPath("$.courtApplication.futureSummonsHearing")
        };

        pollForCourtApplication(applicationId, applicationMatchersForInitate);

        updateCourtApplication(applicationId, "", caseId, "", hearingId, "applications/progression.edit-court-proceedings-for-summons-application.json");

        verifyInMessagingQueueForCourtApplicationUpdated();

        final Matcher[] applicationMatchersForUpdate = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.applicant.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for Summons application")),
                withJsonPath("$.courtApplication.futureSummonsHearing.courtCentre.code", Matchers.is("B01LY00")),
                withJsonPath("$.courtApplication.futureSummonsHearing.judiciary[0].judicialId", Matchers.is("f8254db1-1683-483e-afb3-b87fde5a0a27")),
                withJsonPath("$.courtApplication.futureSummonsHearing.earliestStartDateTime", Matchers.is("2020-12-21T05:27:17.210Z")),
                withJsonPath("$.courtApplication.futureSummonsHearing.estimatedMinutes", Matchers.is(20)),
                withJsonPath("$.courtApplication.futureSummonsHearing.jurisdictionType", Matchers.is("MAGISTRATES")),
                withJsonPath("$.courtApplication.futureSummonsHearing.weekCommencingDate.duration", Matchers.is(20))
        };

        pollForCourtApplication(applicationId, applicationMatchersForUpdate);
    }

    @Test
    public void shouldCreateStandaloneApplicationWithSummons() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-summons-stand-alone-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.applicant.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.summonsRequired", is(true)),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for Summons application"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateLinkedApplicationWithSummonsWithCases() throws Exception {
        final String applicationId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        setupSjpProsecutionCaseQueryStub(caseId, randomUUID().toString());
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-application-with-sjp-case.json");

        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
        };

        pollForCourtApplicationCase(caseId);
        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateDifferentCourtApplicationWithSameCase() throws Exception {
        String applicationId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String prosecutionAuthorityReference = randomUUID().toString();
        setupSjpProsecutionCaseQueryStub(caseId, prosecutionAuthorityReference);
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-application-with-sjp-case.json");

        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
        };

        pollForCourtApplication(applicationId, applicationMatchers);
        pollForCourtApplicationCase(caseId);

        applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-application-with-sjp-case.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] newApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
        };
        pollForCourtApplication(applicationId, newApplicationMatchers);
        pollForCourtApplicationCase(caseId);
    }

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(10, is(applicationReference.length()));
    }

    private void verifyInMessagingQueueForCourtApplicationUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationUpdated);
        assertTrue(message.isPresent());
        String outOfTimeReasons = message.get().getJsonObject("courtApplication").getString("outOfTimeReasons");
        assertThat(outOfTimeReasons, equalTo("Out of times reasons for Summons application"));
    }
}
