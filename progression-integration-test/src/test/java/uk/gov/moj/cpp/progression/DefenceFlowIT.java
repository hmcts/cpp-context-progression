package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import static com.google.common.collect.Lists.newArrayList;
import com.jayway.jsonpath.ReadContext;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import org.apache.http.HttpStatus;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import org.json.JSONException;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

@SuppressWarnings("squid:S1607")
public class DefenceFlowIT extends AbstractIT {

    private static final String PUBLIC_DEFENCE_DEFENCE_ORGANISATION_DISASSOCIATED = "public.defence.defence-organisation-disassociated";
    private static final String PUBLIC_DEFENCE_DEFENCE_ORGANISATION_ASSOCIATED = "public.defence.defence-organisation-associated";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGAL_AID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";

    private final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
    private JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGAL_AID_STATUS_UPDATED).getMessageConsumerClient();


    private static final String statusCode = "G2";
    private static final String userId = UUID.randomUUID().toString();
    private static final String laaContractNumber = "LAA3456";
    private static final String organisationId = UUID.randomUUID().toString();
    private static final String organisationName = "Organisation1 Ltd.";
    private static final String laaContractNumber2 = "LAA1234";
    private static final String organisationId2 = UUID.randomUUID().toString();
    private static final String organisationName2 = "Organisation2 Ltd.";

    private final String caseId = randomUUID().toString();
    private final String defendantId = randomUUID().toString();
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";


    @BeforeAll
    public static void setUpClass() {
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        stubEnableAllCapabilities();
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, organisationId, organisationName);
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber2, organisationId2, organisationName2);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
    }

    @Test
    public void shouldSuccessfullyDisassociateDefenceOrganisationFromDefenceWhenReceiveRepresentationOrderFirst() throws Exception {
        //Create case
        createCase();

        //Receive Rep Order
        receiveRepOrder(laaContractNumber);

        List<Matcher<? super ReadContext>> defenceOrganisationMatcher = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation"));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, defenceOrganisationMatcher));

        //Send public event to disassociate defence organisation
        sendPublicEvent(PUBLIC_DEFENCE_DEFENCE_ORGANISATION_DISASSOCIATED, userId, createPayloadForDisassociation(false));

        defenceOrganisationMatcher = newArrayList(
                withoutJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation"));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, defenceOrganisationMatcher));

    }

    @Test
    public void shouldSuccessfullyReceiveRepresentationOrderWheDefenceOrganisationAssociationFromDefencePerformedFirst() throws Exception {
        //Create case
        createCase();

        //Send public event to associate defence organisation
        sendPublicEvent(PUBLIC_DEFENCE_DEFENCE_ORGANISATION_ASSOCIATED, userId, createPayloadForAssociation(false, laaContractNumber));

        List<Matcher<? super ReadContext>> defenceOrganisationMatcher = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.laaContractNumber", is(laaContractNumber)));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, defenceOrganisationMatcher));

        //Receive Rep Order
        receiveRepOrder(laaContractNumber2);

        defenceOrganisationMatcher = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.laaContractNumber", is(laaContractNumber2)));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, defenceOrganisationMatcher));

    }

    @Test
    public void shouldSuccessfullyDisassociateFromDefenceWhenDefenceOrganisationAssociationFromDefencePerformedFirst() throws Exception {
        //Create case
        createCase();

        //Send public event to associate defence organisation
        sendPublicEvent(PUBLIC_DEFENCE_DEFENCE_ORGANISATION_ASSOCIATED, userId, createPayloadForAssociation(false, laaContractNumber));

        List<Matcher<? super ReadContext>> defenceOrganisationMatcher = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.laaContractNumber", is(laaContractNumber)));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, defenceOrganisationMatcher));

        //Send public event to disassociate defence organisation
        sendPublicEvent(PUBLIC_DEFENCE_DEFENCE_ORGANISATION_DISASSOCIATED, userId, createPayloadForDisassociation(false));

        defenceOrganisationMatcher = newArrayList(
                withoutJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation"));
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, defenceOrganisationMatcher));

    }

    private void createCase() throws IOException, JSONException {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    private void receiveRepOrder(final String laaContractNumber2) {
        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber2, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();
    }

    private void verifyInMessagingQueueForDefendantOffenceUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReference);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertFalse(message.get().containsKey("addedOffences"));
        assertFalse(message.get().containsKey("deletedOffences"));
    }

    private void verifyInMessagingQueueForDefendantLegalAidStatusUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    private JsonObject createPayloadForDisassociation(final boolean isLAA) {
        return Json.createObjectBuilder()
                .add("userId", userId)
                .add("defendantId", defendantId)
                .add("organisationId", organisationId)
                .add("caseId", caseId)
                .add("endDate", "2011-12-03T10:15:30+01:00")
                .add("isLAA", isLAA)
                .build();
    }

    private JsonObject createPayloadForAssociation(final boolean isLAA, final String laaContractNumber) {
        return Json.createObjectBuilder()
                .add("defendantId", defendantId)
                .add("organisationId", organisationId)
                .add("organisationName", organisationName)
                .add("representationType", "PRIVATE")
                .add("caseId", caseId)
                .add("startDate", "2011-12-03T10:15:30+01:00")
                .add("laaContractNumber", laaContractNumber)
                .add("isLAA", isLAA)
                .build();
    }

    private void sendPublicEvent(final String eventName, final String userId, final JsonObject hearingConfirmedJson) {
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(buildMetadata(eventName, userId), hearingConfirmedJson));
    }

}
