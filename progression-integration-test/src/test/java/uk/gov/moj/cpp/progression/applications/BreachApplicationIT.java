package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.addBreachApplicationForExistingHearing;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.progression.AbstractIT;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public class BreachApplicationIT extends AbstractIT {

    @BeforeAll
    public static void setUpClass() {
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
    }

    @Test
    public void shouldCreateLinkedApplicationWithBreachOrder() throws Exception {
        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-breach-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.breachType", is("GENERIC_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out time reasons for Breach order"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateApplicationWithBreachOrderFromExistingHearing() throws Exception {


        final String caseId = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();


        addProsecutionCaseToCrownCourt(caseId, masterDefendantId);

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, masterDefendantId);

        addBreachApplicationForExistingHearing(hearingId, masterDefendantId, "applications/progression.add-breach-application.json");
        final String casePayload = pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.linkedApplicationsSummary", hasSize(1)));
        final String applicationId = new StringToJsonObjectConverter().convert(casePayload).getJsonArray("linkedApplicationsSummary").getJsonObject(0).getString("applicationId");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("CJ03506")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.breachType", is("COMMISSION_OF_NEW_OFFENCE_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.respondents[0].id", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder", notNullValue()),
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateStandaloneApplicationWithBreachOrder() throws Exception {
        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-stand-alone-breach-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.type.breachType", is("GENERIC_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out time reasons for Breach order"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }
}
