package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class StandaloneApplicationIT extends ApplicationsIT {

    @Test
    public void shouldInitiateCourtProceedingsForCourtHearing() throws Exception {
        final String applicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-2.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-standard-organization-prosecution-authority.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithNonStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-non-standard-organization-prosecution-authority.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertionForNonStandardProsecutionAuthority(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithRespondentStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-respondent-standard-organization-prosecution-authority.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertionwithOrganiztionProsectionAuthorityRespondent(applicationId, "UN_ALLOCATED", null);
        pollForCourtApplication(applicationId, applicationMatchers);

    }


    @Test
    public void shouldInitiateCourtProceedingsWithRespondentNonStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-respondent-non-standard-organization-prosecution-authority.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertionwithOrganiztionProsectionAuthorityRespondent(applicationId, "UN_ALLOCATED", "Org name");
        pollForCourtApplication(applicationId, applicationMatchers);

    }


    @Test
    public void shouldInitiateCourtProceedingsWithNonStandardIndividualProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-non-standard-individual-prosecution-authority.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertionForIndividualProsecutionAuthority(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldInitiateCourtProceedingsForBoxHearing() throws Exception {
        final String applicationId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        assertThatRequestIsAccepted(initiateCourtProceedingsForCourtApplication(applicationId, caseId,
                "applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json"));
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "IN_PROGRESS");
        pollForCourtApplication(applicationId, applicationMatchers);
    }

    private Matcher[] createMatchersForAssertion(final String applicationId, final String applicationStatus) {

        return new Matcher[]{
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.applicationStatus", is(applicationStatus)),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for standalone test"))
        };
    }

    private Matcher[] createMatchersForAssertionForNonStandardProsecutionAuthority(final String applicationId, final String applicationStatus) {
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, applicationStatus);
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority", notNullValue()));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is("bdc190e7-c939-37ca-be4b-9f615d6ef40f")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.address.address1", is("176A Lavender Hill")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.name", is("Org name")));
        return applicationMatchers;
    }

    private Matcher[] createMatchersForAssertionForIndividualProsecutionAuthority(final String applicationId, final String applicationStatus) {
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, applicationStatus);
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority", notNullValue()));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is("bdc190e7-c939-37ca-be4b-9f615d6ef40f")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.address.address1", is("176A Lavender Hill")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.firstName", is("Firstname")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.middleName", is("Middlename")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.lastName", is("Lastname")));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.applicant.prosecutingAuthority.address.postCode", is("SW11 1JU")));
        return applicationMatchers;
    }

    private Matcher[] createMatchersForAssertionwithOrganiztionProsectionAuthorityRespondent(final String applicationId, final String applicationStatus, String orgName) {
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, applicationStatus);
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.respondents", notNullValue()));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority", notNullValue()));
        ArrayUtils.add(applicationMatchers, withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.name", is(orgName != null ? orgName : "Transport for London")));
        return applicationMatchers;
    }
}
