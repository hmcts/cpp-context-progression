package uk.gov.moj.cpp.progression;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplicationWithRespondents;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

public class CourtApplicationSubjectCustodialEstablishmentUpdatedIT extends AbstractIT {

    private String hearingId;
    private String defendantId;
    private ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String courtApplicationId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        hearingId = randomUUID().toString();
        courtApplicationId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldUpdateDefendantsDetails_WithNonEmptyCustodyEstablishment_WithEmptyCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();

        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // initiation of  application
        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.initiate-court-proceedings-for-application.json");
        pollForApplication(courtApplicationId);

        helper.updateDefendantWithCustodyEstablishmentInfo(caseId, defendantId, masterDefendantId);

        final Matcher[] defendantWithCustodialEstablishmentMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollProsecutionCasesProgressionFor(caseId, defendantWithCustodialEstablishmentMatchers);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplication = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplication);

        helper.updateDefendantWithEmptyCustodyEstablishmentInfo(caseId, defendantId, masterDefendantId);

        final Matcher[] matchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        pollProsecutionCasesProgressionFor(caseId, matchers);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody"),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment);
    }

    @Test
    public void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddress() throws Exception {
        final String masterDefendantId = defendantId;
        String defendantId1 = randomUUID().toString();
        String defendantId2 = randomUUID().toString();

        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // initiation of  application
        intiateCourtProceedingForApplicationWithRespondents(defendantId1, defendantId2, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(defendantId1, defendantId2, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        pollForApplicationAtAGlance(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    @Test
    void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddressForCourtOrder() throws Exception {
        final String masterDefendantId = defendantId;
        String id2 = randomUUID().toString();
        String id3 = randomUUID().toString();

        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        pollProsecutionCasesProgressionForCAAG(caseId, withJsonPath("$", not(emptyOrNullString())));

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // initiation of  application
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-court-order-linked-application-for-updated-address.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        pollForApplicationAtAGlance(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

}
