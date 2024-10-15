package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoEmailNotificationIsRaised;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumberAsEmpty;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import com.jayway.jsonpath.Filter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReceiveRepresentationOrderIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveRepresentationOrderIT.class.getCanonicalName());
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

    private final String userId = UUID.randomUUID().toString();
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


    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisation_SendCPSNotification() throws Exception {
        final String courtCentreId = UUID.randomUUID().toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);

        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId);
        final String hearingIdForHearing = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingIdForHearing, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyHearingInitialised(caseId, hearingIdForHearing);

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

        verifyEmailNotificationIsRaisedWithoutAttachment(Arrays.asList("SE14 2AB", "Legal House", "15 Sewell Street", "Hammersmith", "joe@example.com", organisationName));
    }


    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisation_DoNotSendCPSNotification_WhenOrganisationWithLAADoNotExists() throws Exception {
        final String courtCentreId = UUID.randomUUID().toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);
        stubGetOrganisationDetailForLAAContractNumberAsEmpty(NO_LAA_CONTRACT_NUMBER_REGISTER);

        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId);
        final String hearingIdForHearing = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingIdForHearing, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyHearingInitialised(caseId, hearingIdForHearing);

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

        verifyNoEmailNotificationIsRaised(Arrays.asList("SE14 2AB", "Legal House", "15 Sewell Street", "Hammersmith", "joe@example.com", organisationName));
    }


    @Test
    public void testReceiveRepresentationWithAssociationOfDefenceOrganisation_DoNotSendCPSNotification_WhenProsecutorIsNotCps() throws Exception {
        final String courtCentreId = UUID.randomUUID().toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", defendantId);

        stubQueryNonCpsProsecutorData("/restResource/referencedata.query.prosecutor-noncps.json", randomUUID());
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId);
        final String hearingIdForHearing = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingIdForHearing, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyHearingInitialised(caseId, hearingIdForHearing);

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

        verifyNoEmailNotificationIsRaised(Arrays.asList("SE14 2AB", "Legal House", "15 Sewell Street", "Hammersmith", "joe@example.com", organisationName));
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
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("FUTURE_HEARING_DATE", futureHearingDate);
        LOGGER.info("Payload: " + strPayload);
        LOGGER.info("COURT_CENTRE_ID==" + courtCentreId);
        LOGGER.info("COURT_CENTRE_NAME==" + courtCentreName);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    private static void verifyHearingInitialised(final String caseId, final String hearingId) {

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(60, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter))
                        )));
    }


    public static void stubQueryNonCpsProsecutorData(final String resourceName, final UUID id) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", id.toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.prosecutor+json");
    }
}
