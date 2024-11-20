package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.ApplicationExternalCreatorType.PROSECUTOR;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class GenericLinkedApplicationIT {

    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(COURT_APPLICATION_CREATED_PRIVATE_EVENT).getMessageConsumerClient();

    @Test
    public void shouldInitiateCourtProceedingsForProsecutionCases() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationExternalCreatorType", is(PROSECUTOR.toString()))
        };

        pollForCourtApplication(applicationId, applicationMatchers);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(1)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("DRAFT"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    @Test
    public void shouldInitiateAppealsApplicationForProsecutionCase() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);

        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-appeal-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationExternalCreatorType", is(PROSECUTOR.toString()))
        };

        pollForCourtApplication(applicationId, applicationMatchers);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(1)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("DRAFT")),
                withJsonPath("$.appealsLodgedInfo[0].defendantId", is("cd3b251d-20e8-44ad-b95e-2f81afde56a4")),
                withJsonPath("$.appealsLodgedInfo[0].offenceIds[0]", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);

        final Matcher[] prosecutionCasesProgressionForCAAG = new Matcher[]{
                withJsonPath("$.defendants[0].id", is(defendantId)),
                withJsonPath("$.appealsLodgedInfo[0].defendantId", is("cd3b251d-20e8-44ad-b95e-2f81afde56a4")),
                withJsonPath("$.appealsLodgedInfo[0].offenceIds[0]", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
        };

        pollProsecutionCasesProgressionForCAAG(caseId, prosecutionCasesProgressionForCAAG);
    }

    @Test
    public void shouldInitiateCourtProceedingsForCourtOrder() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.id", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.orderingCourt.code", is("B01LY00")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationExternalCreatorType", is(PROSECUTOR.toString()))
        };

        pollForCourtApplication(applicationId, applicationMatchers);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationId", is(applicationId)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("UN_ALLOCATED"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference, is(notNullValue()));
    }
}
