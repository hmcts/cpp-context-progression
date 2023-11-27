package uk.gov.moj.cpp.progression;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyHearingApplicationLinkCreated;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyHearingInMessagingQueueForReferToCourt;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyPublicEventForHearingExtended;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

public class CourtApplicationSubjectCustodialEstablishmentUpdatedIT extends AbstractIT {

    private String hearingId;
    private String defendantId;
    ProsecutionCaseUpdateDefendantHelper helper;
    private String courtCentreId;
    private  String caseId;
    private String courtApplicationId;
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application.json";



    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        stubInitiateHearing();
        hearingId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = UUID.randomUUID().toString();
    }



    @Test
    public void shouldUpdateDefendantsDetails_WithNonEmptyCustodyEstablishment_WithEmptyCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();


        // initiation of  case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        // initiation of  application
        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId,masterDefendantId,hearingId, "applications/progression.initiate-court-proceedings-for-application.json");
        verifyHearingInMessagingQueueForReferToCourt();
        verifyHearingApplicationLinkCreated(hearingId);

        helper.updateDefendantWithCustodyEstablishmentInfo(caseId , defendantId, masterDefendantId);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(caseId, custodyEstablishmentDefendantUpdatedMatchers);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplication = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplication);

        helper.updateDefendantWithEmptyCustodyEstablishmentInfo(caseId , defendantId, masterDefendantId);


        final Matcher[] defendantUpdatedMatchersEmptyCustodyEstablishment = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment = new Matcher[]{
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchersEmptyCustodyEstablishment);
        pollProsecutionCasesProgressionFor(caseId, custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplicationMatecherEmptyCustodyEstablishment= {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody"),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplicationMatecherEmptyCustodyEstablishment);




    }

    private void verifyInMessagingQueueForProsecutionCaseCreated(final MessageConsumer publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }
}
