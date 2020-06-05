package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumberAsEmpty;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

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
import org.junit.Before;
import org.junit.Test;

public class ReceiveRepresentationOrderIT extends AbstractIT {

    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    static final String PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA = "public.progression.defence-association-for-laa-locked";
    static final String PUBLIC_PROGRESSION_DEFENCE_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";
    static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED = "public.progression.defence-organisation-for-laa-associated";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForAssociationLockedReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA);

    private static final MessageConsumer messageConsumerClientPublicForLaaContractAssociation = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_LAA_CONTRACT_ASSOCIATED);

    private static final MessageConsumer messageConsumerClientPublicForDefenceOrganisationDisassociation = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_DISASSOCIATED);

    private static final MessageConsumer messageConsumerClientPublicForDefenceOrganisationAssociation = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED);

    final String userId = UUID.randomUUID().toString();
    final String userId2 = UUID.randomUUID().toString();

    String organisationId1;
    String organisationId2;
    final String organisationName1 = "Smith Associates Ltd.";
    final String organisationName2 = "Greg Associates Ltd.";
    private String caseId;
    private String defendantId;
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private final String statusCode = "G2";
    private final String laaContractNumber = "LAA3456";
    private static final String NO_LAA_CONTRACT_NUMBER_REGISTER = "LAA12345";


    @Before
    public void setUp() {
        organisationId1 = UUID.randomUUID().toString();
        organisationId2 = UUID.randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, organisationId2, organisationName2);
        stubGetOrganisationQuery(userId, organisationId1, organisationName1);
        stubGetOrganisationDetails(organisationId1, organisationName1);
        stubEnableAllCapabilities();
    }


    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForRecordLAAReference.close();
        messageConsumerClientPublicForDefendantLegalAidStatusUpdated.close();
        messageConsumerClientPublicForAssociationLockedReference.close();
        messageConsumerClientPublicForLaaContractAssociation.close();
        messageConsumerClientPublicForDefenceOrganisationDisassociation.close();
        messageConsumerClientPublicForDefenceOrganisationAssociation.close();
    }

    private static void verifyInMessagingQueueForLAAContractAssociation(String defendantId, String laaContractNumber, boolean isAssociation) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForLaaContractAssociation);
        assertThat(message.isPresent(), is(true));
        final JsonObject request = message.get();

        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("laaContractNumber"), is(laaContractNumber));
        assertThat(request.getBoolean("isAssociatedByLAA"), is(isAssociation));
    }


    private static void verifyInMessagingQueueFoDefenceDisassociation(String defendantId, String prosecutionCaseId, String organisationId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefenceOrganisationDisassociation);
        assertThat(message.isPresent(), is(true));
        final JsonObject request = message.get();

        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("caseId"), is(prosecutionCaseId));
        assertThat(request.getString("organisationId"), is(organisationId));
    }

    private static void verifyInMessagingQueueFoDefenceAssociation(String defendantId, String organisationId, String laaContractNumber) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefenceOrganisationAssociation);
        assertThat(message.isPresent(), is(true));
        final JsonObject request = message.get();

        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("organisationId"), is(organisationId));
        assertThat(request.getString("laaContractNumber"), is(laaContractNumber));
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertThat(message.get().containsKey("addedOffences"), is(false));
        assertThat(message.get().containsKey("deletedOffences"), is(false));

    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertThat(message.isPresent(), is(true));
    }

    private static void verifyInMessagingQueueForrAssociationLockedForLAA() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForAssociationLockedReference);
        assertThat(message.isPresent(), is(true));
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndDisassociationOfExistingOne() throws Exception {
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        stubGetOrganisationQuery(userId2, organisationId2, organisationName2);
        stubGetOrganisationDetails(organisationId2, organisationName2);
        stubGetUsersAndGroupsQueryForSystemUsers(userId2);
        stubGetGroupsForLoggedInQuery(userId2);


        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        //Receive Representation Order
        //when

        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId2);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
        verifyInMessagingQueueForrAssociationLockedForLAA();
        verifyInMessagingQueueFoDefenceDisassociation(defendantId, caseId, "2fc69990-bf59-4c4a-9489-d766b9abde9a");
        verifyInMessagingQueueFoDefenceAssociation(defendantId, organisationId2, laaContractNumber);
        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final String hearingId = prosecutioncasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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

    }


    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationNotRegister() throws Exception {

        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        stubGetOrganisationQuery(userId2, organisationId2, organisationName2);
        stubGetOrganisationDetails(organisationId2, organisationName2);
        stubGetUsersAndGroupsQueryForSystemUsers(userId2);
        stubGetGroupsForLoggedInQuery(userId2);


        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);


        //Receive Representation Order
        //when

        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, NO_LAA_CONTRACT_NUMBER_REGISTER, userId2);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
        verifyInMessagingQueueForrAssociationLockedForLAA();
        verifyInMessagingQueueForLAAContractAssociation(defendantId, NO_LAA_CONTRACT_NUMBER_REGISTER, false);
        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final String hearingId = prosecutioncasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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

    }


    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndNoAssociationAlreadyExist() throws Exception {
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        stubGetOrganisationQuery(userId2, organisationId2, organisationName2);
        stubGetOrganisationDetails(organisationId2, organisationName2);
        stubGetUsersAndGroupsQueryForSystemUsers(userId2);
        stubGetGroupsForLoggedInQuery(userId2);


        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        //Receive Representation Order
        //when

        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId2);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
        verifyInMessagingQueueForrAssociationLockedForLAA();

        verifyInMessagingQueueFoDefenceAssociation(defendantId, organisationId2, laaContractNumber);
        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final String hearingId = prosecutioncasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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
    }



    @Test
    public void testReceiveRepresentationWithAssociationOfNoRegisterDefenceOrganisationAndAssociationAlreadyExist() throws Exception {
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        stubGetOrganisationQuery(userId2, organisationId2, organisationName2);
        stubGetOrganisationDetails(organisationId2, organisationName2);
        stubGetUsersAndGroupsQueryForSystemUsers(userId2);
        stubGetGroupsForLoggedInQuery(userId2);


        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);
        //Receive Representation Order
        //when

        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, NO_LAA_CONTRACT_NUMBER_REGISTER, userId2);
        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
        verifyInMessagingQueueForrAssociationLockedForLAA();
        verifyInMessagingQueueForLAAContractAssociation(defendantId, NO_LAA_CONTRACT_NUMBER_REGISTER, false);
        verifyInMessagingQueueFoDefenceDisassociation(defendantId, caseId, "2fc69990-bf59-4c4a-9489-d766b9abde9a");
        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final String hearingId = prosecutioncasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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
    }

}