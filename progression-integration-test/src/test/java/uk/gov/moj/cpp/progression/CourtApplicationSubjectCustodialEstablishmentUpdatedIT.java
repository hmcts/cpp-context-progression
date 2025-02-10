package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplicationWithRespondents;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_APPLICATION_AAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
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
        stubInitiateHearing();
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

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(defendantId1, defendantId2, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    @Test
    void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddressForCourtOrder() throws Exception {
        final String masterDefendantId = defendantId;
        String id2 = randomUUID().toString();
        String id3 = randomUUID().toString();

        // initiation of  case
        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();

        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        String res = getProgressionCaseHearings(caseId);
        assertFalse(res.isEmpty());

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        // initiation of  application
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-court-order-linked-application-for-updated-address.json");

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated(final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }

    private String getProgressionCaseHearings(final String caseId, final Matcher... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId,
                PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON, randomUUID().toString(), matchers);
    }

    private String getProgressionQueryForApplicationAtAaag(final String applicationId, final Matcher... matchers) {
        return pollForResponse("/applications/" + applicationId,
                PROGRESSION_QUERY_APPLICATION_AAAG_JSON, randomUUID().toString(), matchers);
    }
}
