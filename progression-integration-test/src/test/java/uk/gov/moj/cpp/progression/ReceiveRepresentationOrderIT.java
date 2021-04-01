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
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ReceiveRepresentationOrderIT extends AbstractIT {

    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGAL_ID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA = "public.progression.defence-association-for-laa-locked";
    private static final String PUBLIC_PROGRESSION_DEFENCE_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED = "public.progression.defence-organisation-for-laa-associated";
    private static final String NO_LAA_CONTRACT_NUMBER_REGISTER = "LAA12345";

    private static final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGAL_ID_STATUS_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForAssociationLockedReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA);
    private static final MessageConsumer messageConsumerClientPublicForLaaContractAssociation = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_LAA_CONTRACT_ASSOCIATED);
    private static final MessageConsumer messageConsumerClientPublicForDefenceOrganisationDisassociation = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_DISASSOCIATED);
    private static final MessageConsumer messageConsumerClientPublicForDefenceOrganisationAssociation = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED);

    private final String userId = UUID.randomUUID().toString();
    private final String organisationName = "Greg Associates Ltd.";
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private final String statusCode = "G2";
    private final String laaContractNumber = "LAA3456";

    private String organisationId;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        organisationId = UUID.randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, organisationId, organisationName);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubEnableAllCapabilities();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageConsumerClientPublicForRecordLAAReference.close();
        messageConsumerClientPublicForDefendantLegalAidStatusUpdated.close();
        messageConsumerClientPublicForAssociationLockedReference.close();
        messageConsumerClientPublicForLaaContractAssociation.close();
        messageConsumerClientPublicForDefenceOrganisationDisassociation.close();
        messageConsumerClientPublicForDefenceOrganisationAssociation.close();
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndDisassociationOfExistingOne() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId);
        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId);

        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        assertInMessagingQueueForDefendantOffenceUpdated();
        assertInMessagingQueueForDefendantLegalAidStatusUpdated();
        assertInMessagingQueueForAssociationLockedForLAA();
        assertInMessagingQueueForDefenceDisassociation(defendantId, caseId, "2fc69990-bf59-4c4a-9489-d766b9abde9a");
        assertMessageForDefenceAssociation(defendantId, organisationId, laaContractNumber);

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        final String response = pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final String hearingId = prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId);
        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, NO_LAA_CONTRACT_NUMBER_REGISTER, userId);

        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        assertInMessagingQueueForDefendantOffenceUpdated();
        assertInMessagingQueueForDefendantLegalAidStatusUpdated();
        assertInMessagingQueueForAssociationLockedForLAA();
        assertInMessagingQueueForLAAContractAssociation(defendantId, NO_LAA_CONTRACT_NUMBER_REGISTER, false);

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        final String response = pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final String hearingId = prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId);
        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId);

        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        assertInMessagingQueueForDefendantOffenceUpdated();
        assertInMessagingQueueForDefendantLegalAidStatusUpdated();
        assertInMessagingQueueForAssociationLockedForLAA();
        assertMessageForDefenceAssociation(defendantId, organisationId, laaContractNumber);

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true)),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.applicationReference", equalTo("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.laaContractNumber", equalTo(laaContractNumber)),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.address.address1", equalTo("address1")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.address.address2", equalTo("address2")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.address.address3", equalTo("address3")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.address.address4", equalTo("address4")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.address.address5", equalTo("address5")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.address.postcode", equalTo("GIR0AA")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.contact.home", equalTo("12346")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.contact.mobile", equalTo("7111133444")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.contact.primaryEmail", equalTo("test@hmcts.net")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.contact.secondaryEmail", equalTo("test@hmcts.net")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.contact.work", equalTo("12345")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.contact.fax", equalTo("1234")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.incorporationNumber", equalTo("7689")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.registeredCharityNumber", equalTo("7654")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.defenceOrganisation.organisation.name", equalTo("Smith Ltd")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", equalTo("REPRESENTATION_ORDER")),
                        withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.isAssociatedByLAA", equalTo(true))
                ));

        final String response = pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final String hearingId = prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId);
        final Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, NO_LAA_CONTRACT_NUMBER_REGISTER, userId);

        assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        assertInMessagingQueueForDefendantOffenceUpdated();
        assertInMessagingQueueForDefendantLegalAidStatusUpdated();
        assertInMessagingQueueForAssociationLockedForLAA();
        assertInMessagingQueueForLAAContractAssociation(defendantId, NO_LAA_CONTRACT_NUMBER_REGISTER, false);
        assertInMessagingQueueForDefenceDisassociation(defendantId, caseId, "2fc69990-bf59-4c4a-9489-d766b9abde9a");

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                asList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("Granted")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true))
                ));

        final String response = pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final String hearingId = prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
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

    private void assertInMessagingQueueForLAAContractAssociation(String defendantId, String laaContractNumber, boolean isAssociation) {
        final Optional<JsonObject> optionalJsonObject = retrieveMessage(messageConsumerClientPublicForLaaContractAssociation);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("laaContractNumber"), is(laaContractNumber));
        assertThat(request.getBoolean("isAssociatedByLAA"), is(isAssociation));
    }

    private void assertInMessagingQueueForDefenceDisassociation(String defendantId, String prosecutionCaseId, String organisationId) {
        final Optional<JsonObject> optionalJsonObject = retrieveMessage(messageConsumerClientPublicForDefenceOrganisationDisassociation);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("caseId"), is(prosecutionCaseId));
        assertThat(request.getString("organisationId"), is(organisationId));
    }

    private void assertMessageForDefenceAssociation(String defendantId, String organisationId, String laaContractNumber) {
        final Optional<JsonObject> optionalJsonObject = retrieveMessage(messageConsumerClientPublicForDefenceOrganisationAssociation);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("organisationId"), is(organisationId));
        assertThat(request.getString("laaContractNumber"), is(laaContractNumber));
    }

    private void assertInMessagingQueueForDefendantOffenceUpdated() {
        final Optional<JsonObject> optionalJsonObject = retrieveMessage(messageConsumerClientPublicForRecordLAAReference);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getJsonArray("updatedOffences").size(), is(1));
        assertThat(request.containsKey("addedOffences"), is(false));
        assertThat(request.containsKey("deletedOffences"), is(false));
    }

    private void assertInMessagingQueueForDefendantLegalAidStatusUpdated() {
        assertThat(retrieveMessage(messageConsumerClientPublicForDefendantLegalAidStatusUpdated).isPresent(), is(true));
    }

    private void assertInMessagingQueueForAssociationLockedForLAA() {
        assertThat(retrieveMessage(messageConsumerClientPublicForAssociationLockedReference).isPresent(), is(true));
    }

    private Optional<JsonObject> retrieveMessage(MessageConsumer messageConsumer) {
        final AtomicReference<Optional<JsonObject>> message = new AtomicReference<>();
        Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> {
            message.set(QueueUtil.retrieveMessageAsJsonObject(messageConsumer));
            return message.get().isPresent();
        });
        return message.get();
    }
}
