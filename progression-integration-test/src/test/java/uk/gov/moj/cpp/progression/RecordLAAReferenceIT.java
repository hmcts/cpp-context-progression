package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReferenceWithUserId;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
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

import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class RecordLAAReferenceIT extends AbstractIT {
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private String caseId;
    private String defendantId;
    private static final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String laaContractNumber = "LAA3456";
    private static final String organisationName = "Greg Associates Ltd.";
    private final String organisationId = UUID.randomUUID().toString();
    private static final String PENDING_STATUS_CODE = "PENDING";

    private String statusCode;
    private String userId;

    @BeforeClass
    public static void setupOnce() {
        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatchWithEmptyResults();
    }

    @AfterClass
    public static void teardownOnce() throws JMSException {
        messageProducerClientPublic.close();
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";

        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatch(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), pncId, croNumber);
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated(final MessageConsumer messageConsumerClientPublicForRecordLAAReference) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertThat(message.get().containsKey("addedOffences"), is(false));
        assertThat(message.get().containsKey("deletedOffences"), is(false));
    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated(final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefenceOrganisationDisassociated(final MessageConsumer messageConsumerClientPublicForLaaDisasociated) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForLaaDisasociated);
        assertTrue(message.isPresent());
    }


    @SuppressWarnings("squid:S1607")
    @Ignore
    @Test
    public void recordLAAReferenceForOffence() throws IOException, JMSException {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        statusCode = "G2";
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        try (final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
             final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED)) {
            //Record LAA reference
            //When
            recordLAAReference(caseId, defendantId, offenceId, statusCode);

            //Then
            verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
            verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        }

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
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, randomUUID().toString(), "Smith Associates Ltd.");

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        try (final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
             final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);
             final MessageConsumer messageConsumerClientPublicForLaaDisasociated = publicEvents.createConsumer(PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED)) {

            //Receive Representation Order
            //when
            final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, "PENDING", laaContractNumber, userId);
            assertThat(responseForRepOrder.getStatus(), equalTo(SC_ACCEPTED));


            //Then
            verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
            verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
            verifyInMessagingQueueForDefenceOrganisationDisassociated(messageConsumerClientPublicForLaaDisasociated);
        }

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

        try (final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
             final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED)) {

            //Record LAA reference
            //When
            stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "WD", "Withdrawn");
            stubGetOrganisationDetailsForUser(userId, organisationId, organisationName);

        Response withdrawResponse = recordLAAReferenceWithUserId(caseId, defendantId, offenceId, statusCode, "Withdrawn", userId);
        assertThat(withdrawResponse.getStatus(), equalTo(SC_ACCEPTED));

            //Then
            verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
            verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        }

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
