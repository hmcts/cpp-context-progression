package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReferenceWithUserId;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatusWithStatusDescription;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailsForUser;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

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
import org.junit.AfterClass;
import org.junit.Test;

public class RecordLAAReferenceIT {
    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    static final String PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForLaaDisasociated = publicEvents
            .createConsumer(PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED);
    private String caseId;
    private String defendantId;
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private String statusCode;
    private final String laaContractNumber = "LAA3456";
    private String organisationId = UUID.randomUUID().toString();
    private final String organisationName = "Greg Associates Ltd.";
    private String userId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForRecordLAAReference.close();
        messageConsumerClientPublicForDefendantLegalAidStatusUpdated.close();
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForRecordLAAReference);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertFalse(message.get().containsKey("addedOffences"));
        assertFalse(message.get().containsKey("deletedOffences"));
    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefenceOrganisationDisassociated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForLaaDisasociated);
        assertTrue(message.isPresent());
    }


    @Test
    public void recordLAAReferenceForOffence() throws IOException {
        userId = UUID.randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        statusCode = "G2";
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        //Record LAA reference
        //When
        recordLAAReference(caseId, defendantId, offenceId, statusCode);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();


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
        stubEnableAllCapabilities();
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "PENDING", "Application Pending");
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        //Receive Representation Order
        //when
        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, "PENDING", laaContractNumber, userId);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("PENDING")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);


        //Record LAA reference
        //When
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "WD", "Withdrawn");
        stubGetOrganisationDetailsForUser(userId, organisationId, organisationName);

        recordLAAReferenceWithUserId(caseId, defendantId, offenceId, "WD", "Withdrawn", userId);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();

        final Matcher[] caseWithDefendantLaaReferenceWithDrawnMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("1980-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("WD")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Withdrawn")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("")),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true)))
        );

        pollProsecutionCasesProgressionFor(caseId, caseWithDefendantLaaReferenceWithDrawnMatchers);

        verifyInMessagingQueueForDefenceOrganisationDisassociated();

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
