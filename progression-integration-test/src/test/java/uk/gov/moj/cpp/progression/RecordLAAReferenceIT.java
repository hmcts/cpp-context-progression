package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReferenceWithUserId;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatusWithStatusDescription;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.removeStub;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatch;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailsForUser;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RecordLAAReferenceIT extends AbstractIT {
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    private String caseId;
    private String defendantId;
    private static final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String laaContractNumber = "LAA3456";
    private static final String organisationName = "Greg Associates Ltd.";
    private final String organisationId = UUID.randomUUID().toString();
    private static final String PENDING_STATUS_CODE = "PENDING";

    private String statusCode;
    private String userId;

    @BeforeAll
    public static void setupOnce() {
        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatchWithEmptyResults();
    }

    @AfterAll
    public static void teardownOnce() throws JMSException {
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";

        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatch(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), pncId, croNumber);
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertThat(message.get().containsKey("addedOffences"), is(false));
        assertThat(message.get().containsKey("deletedOffences"), is(false));
    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefenceOrganisationDisassociated(final JmsMessageConsumerClient messageConsumerClientPublicForLaaDisasociated) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForLaaDisasociated);
        assertTrue(message.isPresent());
    }


    @SuppressWarnings("squid:S1607")
    @Test
    public void recordLAAReferenceForOffence() throws IOException, JMSException, JSONException {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        statusCode = "G2";
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();

        //Record LAA reference
        //When
        recordLAAReference(caseId, defendantId, offenceId, statusCode);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);

        final String hearingId = prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
                .getJsonObject(0).getJsonArray("hearingIds").getString(0);
        getHearingForDefendant(hearingId, getHearingMatchers());
    }

    @Test
    public void recordLAAReferenceWithStatusAsWithDrawn() throws Exception {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        statusCode = "WD";
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", PENDING_STATUS_CODE, "Application Pending");

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, randomUUID().toString(), "Smith Associates Ltd.");

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForLaaDisasociated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED).getMessageConsumerClient();

        //Receive Representation Order
        //when
        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, "PENDING", laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(SC_ACCEPTED));
        }


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        verifyInMessagingQueueForDefenceOrganisationDisassociated(messageConsumerClientPublicForLaaDisasociated);

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is(PENDING_STATUS_CODE)),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true)),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.applicationReference", equalTo("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.isAssociatedByLAA", equalTo(true)),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.laaContractNumber", equalTo(laaContractNumber))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference2 = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated2 = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();


        //Record LAA reference
        //When
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "WD", "Withdrawn");
        stubGetOrganisationDetailsForUser(userId, organisationId, organisationName);

        try (Response withdrawResponse = recordLAAReferenceWithUserId(caseId, defendantId, offenceId, statusCode, "Withdrawn", userId)) {
            assertThat(withdrawResponse.getStatus(), equalTo(SC_ACCEPTED));
        }

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference2);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated2);

        final Matcher[] caseWithDefendantLaaReferenceWithDrawnMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("1980-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is(statusCode)),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Withdrawn")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(false)))
        );

        pollProsecutionCasesProgressionFor(caseId, caseWithDefendantLaaReferenceWithDrawnMatchers);
    }

    private Matcher[] getHearingMatchers() {
        final List<Matcher> matchers = newArrayList(withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("1980-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        );
        return matchers.toArray(new Matcher[0]);
    }

}
