package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.helper.RestHelper;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseAtAGlanceIT extends AbstractIT {
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS = "progression.command.initiate-court-proceedings.json";
    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String linkedApplicationId;


    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        linkedApplicationId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
    }

    @Test
    public void shouldVerifyCaseDetailsForCaseAtAGlance() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        verifyCaseAtAGlance(caseId, defendantDOB);
    }

    private static void verifyCaseAtAGlance(final String caseId, final String defendantDOB) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.caseDetails.caseMarkers[0]", equalTo("Prohibited Weapons")),

                                withJsonPath("$.prosecutorDetails.prosecutionAuthorityReference", notNullValue()),
                                withJsonPath("$.prosecutorDetails.prosecutionAuthorityCode", equalTo("TFL")),
                                withJsonPath("$.prosecutorDetails.prosecutionAuthorityId", equalTo("cf73207f-3ced-488a-82a0-3fba79c2ce85")),
                                withJsonPath("$.prosecutorDetails.address.address1", equalTo("6th Floor Windsor House")),
                                withJsonPath("$.prosecutorDetails.address.address2", equalTo("42-50 Victoria Street")),
                                withJsonPath("$.prosecutorDetails.address.address3", equalTo("London")),
                                withJsonPath("$.prosecutorDetails.address.postcode", equalTo("SW1H 0TL")),

                                withJsonPath("$.defendants[0].firstName", equalTo("Harry")),
                                withJsonPath("$.defendants[0].lastName", equalTo("Kane Junior")),
                                withJsonPath("$.defendants[0].interpreterLanguageNeeds", equalTo("Welsh")),
                                withJsonPath("$.defendants[0].remandStatus", equalTo("Remanded into Custody")),
                                withJsonPath("$.defendants[0].address.address1", equalTo("22")),
                                withJsonPath("$.defendants[0].address.address2", equalTo("Acacia Avenue")),
                                withJsonPath("$.defendants[0].dateOfBirth", equalTo(defendantDOB)),
                                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceCode", equalTo("TTH105HY")),
                                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceTitle", equalTo("ROBBERY")),
                                withJsonPath("$.defendants[0].caagDefendantOffences[0].wording", equalTo("No Travel Card")),
                                withJsonPath("$.defendants[0].caagDefendantOffences[0].wordingWelsh", equalTo("No Travel Card In Welsh"))
                        )));
    }

    @Test
    public void shouldVerifyCaseAtAGlanceLinkedApplication() throws Exception {
        createApplicationLinkedToCase();

        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.linkedApplications[0].applicationId", equalTo(linkedApplicationId)),
                                withJsonPath("$.linkedApplications[0].applicationTitle", equalTo("Application for bad character")),
                                withJsonPath("$.linkedApplications[0].applicantDisplayName", equalTo("Applicant Organisation")),
                                withJsonPath("$.linkedApplications[0].applicationStatus", equalTo("DRAFT")),
                                withJsonPath("$.linkedApplications[0].respondentDisplayNames[0]", equalTo("Respondent Organisation"))
                        )));
    }

    public void createApplicationLinkedToCase() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        addCourtApplication(caseId, linkedApplicationId, "progression.command.create-court-application.json");

        Matcher[] linkedApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(linkedApplicationId)),
                withJsonPath("$.courtApplication.linkedCaseId", is(caseId))
        };

        pollForApplication(linkedApplicationId, linkedApplicationMatchers);

    }

}

