package uk.gov.moj.cpp.progression;

import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationDisassociatedDataPersistedForRepOrder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

public class ReceiveRepresentationOrderIT {

    private String caseId = randomUUID().toString();
    private String defendantId = randomUUID().toString();
    private String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private String statusCode = "G2";
    private String laaContractNumber = "LAA3456";
    final String userId = UUID.randomUUID().toString();
    final String userId2 = UUID.randomUUID().toString();

    final String organisationId1 = UUID.randomUUID().toString();
    final String organisationId2 = UUID.randomUUID().toString();
    final String organisationName1 = "Smith Associates Ltd.";
    final String organisationName2 = "Greg Associates Ltd.";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";


    private static final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);


    @Before
    public void setUp() {
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, organisationId2, organisationName2);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId1, organisationName1);
        stubGetOrganisationDetails(organisationId1, organisationName1);

    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForRecordLAAReference.close();
        messageConsumerClientPublicForDefendantLegalAidStatusUpdated.close();
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndDisassociationOfExistingOne () throws Exception {
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {
            //When
            associateOrganisation(defendantId, userId);

            //Then
             helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId1);
            verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                    organisationId1,
                    userId);
        }

        stubGetOrganisationQuery(userId2, organisationId2, organisationName2);
        stubGetOrganisationDetails(organisationId2, organisationName2);
        stubGetUsersAndGroupsQueryForSystemUsers(userId2);
        stubGetGroupsForLoggedInQuery(userId2);


        //Receive Representation Order
        //when
        Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId2);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

         response = pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final String hearingId = prosecutioncasesJsonObject.getJsonObject("caseAtAGlance").getJsonArray("defendantHearings")
                .getJsonObject(0).getJsonArray("hearingIds").getString(0);

        final String hearingResponse = getHearingForDefendant(hearingId);
        final JsonObject hearingJsonObject = getJsonObject(hearingResponse);
        assertThat(hearingJsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonObject("laaApplnReference").getString("applicationReference"), equalTo("AB746921"));
        assertThat(hearingJsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonObject("laaApplnReference").getString("statusCode"), equalTo("G2"));
        assertThat(hearingJsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonObject("laaApplnReference").getString("statusDescription"), equalTo("Application Pending"));

        assertThat(hearingJsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonObject("laaApplnReference").getString("statusDate"), equalTo("2019-07-15"));
        assertThat(hearingJsonObject.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getString("legalAidStatus"), equalTo("Granted"));
        verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                organisationId2,
                userId);
        verifyDefenceOrganisationDisassociatedDataPersistedForRepOrder(defendantId,
                organisationId1,
                userId);


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


}
