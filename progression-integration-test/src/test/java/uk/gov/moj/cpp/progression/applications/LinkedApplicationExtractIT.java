package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.addBreachApplicationForExistingHearing;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.getLinkedApplicationExtractPdf;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.AbstractIT;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LinkedApplicationExtractIT extends AbstractIT {

    @BeforeAll
    public static void setUpClass() {
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
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

        final String linkedApplicationExtractPdf = getLinkedApplicationExtractPdf(applicationId, masterDefendantId, hearingId);
        assertThat(linkedApplicationExtractPdf, Matchers.is(Matchers.notNullValue()));
    }

}
