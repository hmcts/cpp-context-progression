package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumberAsEmpty;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.util.FileUtil;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReceiveRepresentationOrderIT extends AbstractIT {

    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGAL_ID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA = "public.progression.defence-association-for-laa-locked";
    private static final String PUBLIC_PROGRESSION_DEFENCE_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    private static final String PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED = "public.progression.defence-organisation-for-laa-associated";
    private static final String NO_LAA_CONTRACT_NUMBER_REGISTER = "LAA12345";

    private static final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGAL_ID_STATUS_UPDATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForAssociationLockedReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENCE_ASSOCIATION_LOCKED_FOR_LAA).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForLaaContractAssociation = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENCE_LAA_CONTRACT_ASSOCIATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForDefenceOrganisationDisassociation = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_DISASSOCIATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForDefenceOrganisationAssociation = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENCE_ORGANISATION_ASSOCIATED).getMessageConsumerClient();
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed-cps-notification.json";

    private final String userId = randomUUID().toString();
    private final String organisationName = RandomStringUtils.randomAlphabetic(10);
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private final String statusCode = "G2";
    private final String laaContractNumber = "LAA3456";
    private final String futureHearingDate = LocalDate.now().plusYears(1) + "T09:30:00.000Z";
    private String organisationId;
    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        organisationId = randomUUID().toString();
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
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndDisassociationOfExistingOne() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
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

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final Matcher<? super ReadContext>[] hearingMatchers = new Matcher[]{
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        };
        getHearingForDefendant(hearingId, hearingMatchers);
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationNotRegister() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, NO_LAA_CONTRACT_NUMBER_REGISTER, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
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

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final Matcher<? super ReadContext>[] hearingMatchers = new Matcher[]{
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        };
        getHearingForDefendant(hearingId, hearingMatchers);
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisationAndNoAssociationAlreadyExist() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
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

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final Matcher<? super ReadContext>[] hearingMatchers = new Matcher[]{
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        };
        getHearingForDefendant(hearingId, hearingMatchers);
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfNoRegisterDefenceOrganisationAndAssociationAlreadyExist() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, NO_LAA_CONTRACT_NUMBER_REGISTER, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
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

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final Matcher<? super ReadContext>[] hearingMatchers = new Matcher[]{
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        };
        getHearingForDefendant(hearingId, hearingMatchers);
    }

    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisation_SendCPSNotification() throws Exception {
        final String courtCentreId = randomUUID().toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);

        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, statusCode, laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }
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

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final Matcher<? super ReadContext>[] hearingMatchers = new Matcher[]{
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        };
        getHearingForDefendant(hearingId, hearingMatchers);

        verifyEmailNotificationIsRaisedWithoutAttachment(Arrays.asList("SE14 2AB", "Legal House", "15 Sewell Street", "Hammersmith", "joe@example.com", organisationName));
    }

    private void assertInMessagingQueueForLAAContractAssociation(String defendantId, String laaContractNumber, boolean isAssociation) {
        final Optional<JsonObject> optionalJsonObject = retrieveMessageBody(messageConsumerClientPublicForLaaContractAssociation);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("laaContractNumber"), is(laaContractNumber));
        assertThat(request.getBoolean("isAssociatedByLAA"), is(isAssociation));
    }

    private void assertInMessagingQueueForDefenceDisassociation(String defendantId, String prosecutionCaseId, String organisationId) {
        final Optional<JsonObject> optionalJsonObject = retrieveMessageBody(messageConsumerClientPublicForDefenceOrganisationDisassociation);
        assertThat(optionalJsonObject.isPresent(), is(true));
        final JsonObject request = optionalJsonObject.get();
        assertThat(request.getString("defendantId"), is(defendantId));
        assertThat(request.getString("caseId"), is(prosecutionCaseId));
        assertThat(request.getString("organisationId"), is(organisationId));
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

    private JsonObject getInstructedJsonObject(final String path, final String caseId, final String hearingId,
                                               final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = FileUtil.getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("FUTURE_HEARING_DATE", futureHearingDate);
        return stringToJsonObjectConverter.convert(strPayload);
    }

}
