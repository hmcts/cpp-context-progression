package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateOffenceWithLAAReferenceIT extends AbstractIT {

    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGAL_ID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA = "public.progression.defence-association-for-laa-locked";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED = "public.progression.defence-organisation-for-laa-associated";
    private static final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGAL_ID_STATUS_UPDATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForAssociationLockedReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForDefenceOrganisationAssociation = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED).getMessageConsumerClient();

    private final String userId = UUID.randomUUID().toString();
    private final String organisationName = "Greg Associates Ltd.";
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private final String statusCode = "G2";
    private final String laaContractNumber = "LAA3456";
    private String organisationId;
    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        organisationId = UUID.randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, organisationId, organisationName);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubEnableAllCapabilities();

        IdMapperStub.setUp();
        stubInitiateHearing();
        stubDocumentCreate(STRING.next());
        NotificationServiceStub.setUp();
    }


    @Test
    public void shouldTestLaaApplicationReferenceAddedToNewlyAddedOffenceIfOneOfTheOffencesAlreadyHasLaaApplicationReference() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId);

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
        assertInMessagingQueueForDefendantOffenceUpdated();
        assertInMessagingQueueForDefendantLegalAidStatusUpdated();
        assertInMessagingQueueForAssociationLockedForLAA();
        assertMessageForDefenceAssociation(defendantId, organisationId, laaContractNumber);

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.applicationReference", equalTo("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.laaContractNumber", equalTo(laaContractNumber)),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.isAssociatedByLAA", equalTo(true)),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2"))
                ));

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, randomUUID().toString());

        //Adding new offence
        helper.updateOffenceOfSingleDefendant();

        helper.verifyInMessagingQueueForOffencesUpdated(messageConsumerClientPublicForRecordLAAReference);

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference", Objects::nonNull),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].laaApplnReference", Objects::nonNull),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[2].id", Objects::nonNull));
        withJsonPath("$.prosecutionCase.defendants[0].offences[2].laaApplnReference", Objects::isNull);
    }


    private void assertMessageForDefenceAssociation(String defendantId, String organisationId, String laaContractNumber) {
        final Optional<JsonObject> optionalJsonObject = retrieveMessageBody(messageConsumerClientPublicForDefenceOrganisationAssociation);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("organisationId"), is(organisationId));
        assertThat(request.getString("laaContractNumber"), is(laaContractNumber));
    }

    private void assertInMessagingQueueForDefendantOffenceUpdated() {
        final Optional<JsonObject> optionalJsonObject = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReference);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getJsonArray("updatedOffences").size(), is(1));
        assertThat(request.containsKey("addedOffences"), is(false));
        assertThat(request.containsKey("deletedOffences"), is(false));
    }

    private void assertInMessagingQueueForDefendantLegalAidStatusUpdated() {
        assertThat(retrieveMessageBody(messageConsumerClientPublicForDefendantLegalAidStatusUpdated).isPresent(), is(true));
    }

    private void assertInMessagingQueueForAssociationLockedForLAA() {
        assertThat(retrieveMessageBody(messageConsumerClientPublicForAssociationLockedReference).isPresent(), is(true));
    }
}
