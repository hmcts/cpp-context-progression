package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetEmptyOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AssociateOrphanedDefendantsIT extends AbstractIT {
    private String caseId = randomUUID().toString();
    private String defendantId = randomUUID().toString();
    private String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private String statusCode = "G2";
    private String laaContractNumber = "LAA3456";
    private static final String PUBLIC_USER_GROUP_ORG_CREATED = "public.usersgroups.organisation-created";

    final String userId = UUID.randomUUID().toString();




    final String organisationId2 = UUID.randomUUID().toString();
    final String organisationName2 = "Greg Associates Ltd.";

    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";


    private static final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents
            .createPublicConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents
            .createPublicConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);


    @Before
    public void setUp() {
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        stubGetEmptyOrganisationDetailForLAAContractNumber(laaContractNumber);
        stubGetOrganisationQuery(userId, organisationId2, organisationName2);
        stubGetOrganisationDetails(organisationId2, organisationName2);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);


    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForRecordLAAReference.close();
        messageConsumerClientPublicForDefendantLegalAidStatusUpdated.close();
    }

    @Ignore("DD-20985")
    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndDisassociationOfExistingOne () throws Exception {
        //Create prosecution case
        //Given
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        //Receive Representation Order
        //when
        Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();




        //Send Public event to associate orphaned Defendant Id

        sendMessage(messageProducerClientPublic,
                PUBLIC_USER_GROUP_ORG_CREATED, createPayloadForOrganisationSetup(), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_USER_GROUP_ORG_CREATED)
                        .withUserId(userId)
                        .build());


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

    private JsonObject createPayloadForOrganisationSetup() {
        return Json.createObjectBuilder()
                .add("organisationDetails", Json.createObjectBuilder()
                    .add(ORGANISATION_ID, organisationId2)
                    .add(ORGANISATION_NAME, organisationName2)
                    .add(LAA_CONTRACT_NUMBER, laaContractNumber)
                    .add("timeTriggered", "2011-12-03T10:15:30+01:00")
                    .add("organisationType", "LEGAL_ORGANISATION")
                    .add("addressLine1", "Address Line1")
                    .add("addressLine4", "Address Line4")
                    .add("addressPostcode", "SE14 2AB")
                    .add("phoneNumber", "080012345678")
                    .add("email", "joe@example.com")
                    .build())
                .build();

    }

}
