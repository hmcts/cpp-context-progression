package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.justice.core.courts.ApplicationExternalCreatorType.PROSECUTOR;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;

import uk.gov.moj.cpp.progression.AbstractIT;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class GenericLinkedApplicationIT extends AbstractIT {

    @Test
    public void shouldInitiateCourtProceedingsForProsecutionCases() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

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
    public void shouldInitiateCourtProceedingsForCourtOrder() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("LISTED")),
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
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("LISTED"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }
}