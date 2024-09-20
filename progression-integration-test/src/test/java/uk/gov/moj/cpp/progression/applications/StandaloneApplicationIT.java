package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class StandaloneApplicationIT {

    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(COURT_APPLICATION_CREATED_PRIVATE_EVENT).getMessageConsumerClient();

    @Test
    public void shouldInitiateCourtProceedingsForCourtHearing() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-2.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-standard-organization-prosecution-authority.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertion(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithNonStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-non-standard-organization-prosecution-authority.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertionForNonStandardProsecutionAuthority(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }

    @Test
    public void shouldInitiateCourtProceedingsWithRespondentStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-respondent-standard-organization-prosecution-authority.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertionwithOrganiztionProsectionAuthorityRespondent(applicationId, "UN_ALLOCATED", null);
        pollForCourtApplication(applicationId, applicationMatchers);

    }


    @Test
    public void shouldInitiateCourtProceedingsWithRespondentNonStandardOrganizationProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-respondent-non-standard-organization-prosecution-authority.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertionwithOrganiztionProsectionAuthorityRespondent(applicationId, "UN_ALLOCATED", "Org name");
        pollForCourtApplication(applicationId, applicationMatchers);

    }


    @Test
    public void shouldInitiateCourtProceedingsWithNonStandardIndividualProsecutionAuthority() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-standalone-application-with-non-standard-individual-prosecution-authority.json");
        verifyCourtApplicationCreatedPrivateEvent();
        final Matcher[] applicationMatchers = createMatchersForAssertionForIndividualProsecutionAuthority(applicationId, "UN_ALLOCATED");
        pollForCourtApplication(applicationId, applicationMatchers);

    }


    @Test
    public void shouldInitiateCourtProceedingsForBoxHearing() throws Exception {
        stubInitiateHearing();
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, UUID.randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");
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

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(10, is(applicationReference.length()));
    }
}
