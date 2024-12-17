package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithPoliceBailInfo;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("squid:S1607")
@ExtendWith(JmsResourceManagementExtension.class)
public class CaseAtAGlanceIT {
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS = "progression.command.initiate-court-proceedings.json";
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_NON_STD_ORGANISATION_PROSECUTOR = "progression.command.initiate-court-proceedings-non-std-organisation.json";
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_WITHOUT_BAIL_CONDITION = "progression.command.initiate-court-proceedings-without-bail-condition.json";
    public static final String PROGRESSION_QUERY_GET_CASE_HEARINGS = "application/vnd.progression.query.casehearings+json";
    public static final String PROGRESSION_QUERY_GET_CASE_DEFENDANT_HEARINGS = "application/vnd.progression.query.case-defendant-hearings+json";
    public static final String PROGRESSION_QUERY_GET_CASE_HEARING_TYPES = "application/vnd.progression.query.case.hearingtypes+json";
    private static final JmsMessageConsumerClient publicEventsCaseDefendantChanged = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-defendant-changed").getMessageConsumerClient();

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String linkedApplicationId;

    ProsecutionCaseUpdateDefendantHelper helper;

    @BeforeEach
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
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);

        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
    }

    @Test
    public void shouldVerifyCaseDetailsForCaseAtAGlance() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        pollProsecutionCasesProgressionForCAAG(caseId,

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
                withJsonPath("$.defendants[0].address.address1", equalTo("22")),
                withJsonPath("$.defendants[0].address.address2", equalTo("Acacia Avenue")),
                withJsonPath("$.defendants[0].dateOfBirth", equalTo(defendantDOB)),
                withJsonPath("$.defendants[0].driverNumber", equalTo("AACC12345")),
                withJsonPath("$.defendants[0].gender", equalTo("MALE")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].id", notNullValue()),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].label", equalTo("Section 49 of the Children and Young Persons Act 1933 applies")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].orderedDate", notNullValue()),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceCode", equalTo("TTH105HY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceTitle", equalTo("ROBBERY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].wording", equalTo("No Travel Card")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].wordingWelsh", equalTo("No Travel Card In Welsh")),
                withJsonPath("$.defendants[0].remandStatus", equalTo("Remanded into Custody"))
        );

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01")));

        verifyCaseHearings(caseId);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailConditions", is("bail conditions...")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description", is("Remanded into Custody")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailConditions", is("bail conditions...")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description", is("Remanded into Custody"))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
    }

    @Test
    public void shouldVerifyCaseDetailsForCaseAtAGlanceIfBailConditionIsNotPresent() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_WITHOUT_BAIL_CONDITION, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        pollProsecutionCasesProgressionForCAAG(caseId,
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
                withJsonPath("$.defendants[0].address.address1", equalTo("22")),
                withJsonPath("$.defendants[0].address.address2", equalTo("Acacia Avenue")),
                withJsonPath("$.defendants[0].dateOfBirth", equalTo(defendantDOB)),
                withJsonPath("$.defendants[0].driverNumber", equalTo("AACC12345")),
                withJsonPath("$.defendants[0].gender", equalTo("MALE")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].id", notNullValue()),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].label", equalTo("Section 49 of the Children and Young Persons Act 1933 applies")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].orderedDate", notNullValue()),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceCode", equalTo("TTH105HY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceTitle", equalTo("ROBBERY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].wording", equalTo("No Travel Card")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].wordingWelsh", equalTo("No Travel Card In Welsh")),
                withoutJsonPath("$.defendants[0].remandStatus")
        );

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01")));
        verifyCaseHearings(caseId);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailConditions"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailConditions"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.id"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description")
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
    }

    @Test
    public void shouldCreateCaseWithPoliceBailInformationAndShouldNotOverwriteWhilstUpdatingDefendant() throws Exception {
        //given
        final UUID policeBailStatusId = randomUUID();
        final String policeBailStatusDesc = "police bail status description";
        final String policeBailConditions = "police bail conditions";

        initiateCourtProceedingsWithPoliceBailInfo(caseId, defendantId, listedStartDateTime, earliestStartDateTime, policeBailStatusId.toString(), policeBailStatusDesc, policeBailConditions);

        final Matcher[] defendantMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description", is("Remanded into Custody"))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantMatchers);

        // when
        final String updateDefendantPoliceBailStatusId = randomUUID().toString();
        final String updateDefendantPoliceBailStatusDesc = "police bail status description for update defendant";
        final String updateDefendantPoliceBailConditions = "police bail conditions for update defendant";

        helper.updateDefendantWithPoliceBailInfo(updateDefendantPoliceBailStatusId, updateDefendantPoliceBailStatusDesc, updateDefendantPoliceBailConditions);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailConditions", is(policeBailConditions)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description", is("Remanded into Custody"))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendantChanged(publicEventsCaseDefendantChanged);
    }

    @Test
    public void shouldNotCallReferenceDataForNonStdOrganisationProsecutor() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_NON_STD_ORGANISATION_PROSECUTOR, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        verifyCaseAtAGlanceForNonStdProsecutor(caseId, "Organisation Address 1", "Organisation Address 2", "SW11 1JU");
        verifyCaseHearings(caseId);
    }

    @Test
    public void shouldReturnProsecutionCaseWithCourtOrders() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        verifyProsecutionCaseCourtOrders(caseId);
    }

    @Test
    public void shouldVerifyCaseDefendantHearings() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        verifyCaseDefendantHearings(caseId, defendantId);

    }

    @Test
    public void shouldVerifyCaseAtAGlanceLinkedApplication() throws Exception {
        createApplicationLinkedToCase();


        pollProsecutionCasesProgressionForCAAG(caseId, withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.linkedApplications[0].applicationId", equalTo(linkedApplicationId)),
                withJsonPath("$.linkedApplications[0].applicationTitle", equalTo("Application for an order of reimbursement in relation to a closure order")),
                withJsonPath("$.linkedApplications[0].applicantDisplayName", equalTo("Applicant Organisation")),
                withJsonPath("$.linkedApplications[0].applicationStatus", equalTo("DRAFT")),
                withJsonPath("$.linkedApplications[0].respondentDisplayNames[0]", equalTo("Respondent Organisation")));

    }

    @Test
    public void shouldVerifyProsecutionCase() throws IOException {
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());

        pollCasesProgressionFor(caseId, prosecutionCaseMatchers);
    }

    @Test
    public void shouldVerifyNoCaseHearingTypesWhenCaseNotConfirmed() throws Exception {
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-07-15T18:32:04.238Z").toString();
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final LocalDate HEARING_DATE_1 = LocalDate.of(2020, 07, 15);
        verifyNoCaseHearingTypes(caseId, HEARING_DATE_1);
    }

    private void createApplicationLinkedToCase() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        initiateCourtProceedingsForCourtApplication(linkedApplicationId, caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        Matcher[] linkedApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(linkedApplicationId)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", is(caseId))
        };

        pollForApplication(linkedApplicationId, linkedApplicationMatchers);

    }

    private void verifyCaseHearings(final String caseId) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_GET_CASE_HEARINGS)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.length()", is(1)),
                                withJsonPath("$.hearings[0].hearingId", is(notNullValue())),
                                withJsonPath("$.hearings[0].courtCentre.id", is("88cdf36e-93e4-41b0-8277-17d9dba7f06f")),
                                withJsonPath("$.hearings[0].courtCentre.name", is("Lavender Hill Magistrate's Court")),
                                withJsonPath("$.hearings[0].courtCentre.roomId", is("9e4932f7-97b2-3010-b942-ddd2624e4dd8")),
                                withJsonPath("$.hearings[0].courtCentre.roomName", is("Courtroom 01"))
                        )));
    }

    private void verifyCaseDefendantHearings(final String caseId, final String defendantId) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), PROGRESSION_QUERY_GET_CASE_DEFENDANT_HEARINGS)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.length()", is(1)),
                                withJsonPath("$.hearings[0].hearingId", is(notNullValue())),
                                withJsonPath("$.hearings[0].courtCentre.id", is("88cdf36e-93e4-41b0-8277-17d9dba7f06f")),
                                withJsonPath("$.hearings[0].courtCentre.name", is("Lavender Hill Magistrate's Court")),
                                withJsonPath("$.hearings[0].courtCentre.roomId", is("9e4932f7-97b2-3010-b942-ddd2624e4dd8")),
                                withJsonPath("$.hearings[0].courtCentre.roomName", is("Courtroom 01")),
                                withJsonPath("$.hearings[0].hearingDays[0].sittingDay", is("2019-05-30T18:32:04.238Z"))
                        )));
    }

    private void verifyCaseAtAGlanceForNonStdProsecutor(final String caseId, final String address1, final String address2, final String postCode) {

        Matcher[] matchers = {
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.caseDetails.caseMarkers[0]", equalTo("Prohibited Weapons")),
                withJsonPath("$.prosecutorDetails.prosecutionAuthorityReference", notNullValue()),
                withJsonPath("$.prosecutorDetails.prosecutionAuthorityCode", equalTo("TFL")),
                withJsonPath("$.prosecutorDetails.prosecutionAuthorityId", equalTo("cf73207f-3ced-488a-82a0-3fba79c2ce85")),
                withJsonPath("$.prosecutorDetails.address.address1", equalTo(address1)),
                withJsonPath("$.prosecutorDetails.address.address2", equalTo(address2)),
                withJsonPath("$.prosecutorDetails.address.postcode", equalTo(postCode)),

                withJsonPath("$.defendants[0].firstName", equalTo("Harry")),
                withJsonPath("$.defendants[0].lastName", equalTo("Kane Junior")),
                withJsonPath("$.defendants[0].interpreterLanguageNeeds", equalTo("Welsh")),
                withJsonPath("$.defendants[0].remandStatus", equalTo("Remanded into Custody")),
                withJsonPath("$.defendants[0].address.address1", equalTo("22")),
                withJsonPath("$.defendants[0].address.address2", equalTo("Acacia Avenue")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].id", notNullValue()),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].label", equalTo("Section 49 of the Children and Young Persons Act 1933 applies")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].reportingRestrictions[0].orderedDate", notNullValue()),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceCode", equalTo("TTH105HY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].offenceTitle", equalTo("ROBBERY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].wording", equalTo("No Travel Card")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].wordingWelsh", equalTo("No Travel Card In Welsh")),
        };
        pollProsecutionCasesProgressionForCAAG(caseId, matchers);
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01")));
    }

    private void verifyProsecutionCaseCourtOrders(final String caseId) {

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.activeCourtOrders[0].masterDefendantId", equalTo(this.defendantId)),
                withJsonPath("$.activeCourtOrders[0].courtOrders[0].id", equalTo("2fc69990-bf59-4c4a-9489-d766b9abde9a")));
    }

    private static void verifyNoCaseHearingTypes(final String caseId, final LocalDate orderDate) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId + "?orderDate=" + orderDate.toString()), PROGRESSION_QUERY_GET_CASE_HEARING_TYPES)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearingTypes.length()", is(0)))));
    }
}

