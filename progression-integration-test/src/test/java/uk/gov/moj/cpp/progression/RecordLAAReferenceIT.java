package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.*;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
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
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.ReadContext;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

public class RecordLAAReferenceIT extends AbstractIT {
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";
    private String caseId;
    private String defendantId;
    private static final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String laaContractNumber = "LAA3456";
    private static final String organisationName = "Greg Associates Ltd.";
    private final String organisationId = UUID.randomUUID().toString();
    private static final String PENDING_STATUS_CODE = "PENDING";

    private String statusCode;
    private String userId;
    private ProsecutionCaseUpdateDefendantHelper helper;
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @BeforeAll
    public static void setupOnce() {
        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatchWithEmptyResults();
    }

    @AfterAll
    public static void teardownOnce() throws JMSException {
        final String pncId = "2099/1234567L";
        final String croNumber = "1234567";

        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatch(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), pncId, croNumber);
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertThat(message.get().containsKey("addedOffences"), is(false));
        assertThat(message.get().containsKey("deletedOffences"), is(false));
    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    @SuppressWarnings("squid:S1607")
    @Test
    public void recordLAAReferenceForOffence() throws IOException, JSONException {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        statusCode = "G2";
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();

        //Record LAA reference
        //When
        recordLAAReference(caseId, defendantId, offenceId, statusCode);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);

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
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, randomUUID().toString(), "Smith Associates Ltd.");

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForLaaDisasociated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED).getMessageConsumerClient();

        //Receive Representation Order
        //when
        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, "PENDING", laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(SC_ACCEPTED));
        }


        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);

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

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference2 = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated2 = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();


        //Record LAA reference
        //When
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "WD", "Withdrawn");
        stubGetOrganisationDetailsForUser(userId, organisationId, organisationName);

        try (Response withdrawResponse = recordLAAReferenceWithUserId(caseId, defendantId, offenceId, statusCode, "Withdrawn", userId)) {
            assertThat(withdrawResponse.getStatus(), equalTo(SC_ACCEPTED));
        }

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference2);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated2);

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

    @Test
    public void shouldSaveCustodyEstablishmentAfterLAA() throws Exception {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        statusCode = "GR";
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        setupAsAuthorisedUser(fromString(userId), "stub-data/usersgroups.get-specific-groups-by-user.json");
        stubGetGroupsForLoggedInQuery(userId);
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "GRANTED", "Granted");

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        resultHearingWithBailAndCustomEstablishment(hearingId);

        verifyHearingWithMatchers(hearingId, new Matcher[]{
                withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults[0].orderedDate", is("2025-08-05")),
        });

        stubGetOrganisationDetailForLAAContractNumber(laaContractNumber, randomUUID().toString(), "Smith Associates Ltd.");
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();

        //Record LAA reference
        //When
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", "GR", "Granted");
        stubGetOrganisationDetailsForUser(userId, organisationId, organisationName);

        try (Response withdrawResponse = recordLAAReferenceWithUserId(caseId, defendantId, offenceId, statusCode, "Granted", userId)) {
            assertThat(withdrawResponse.getStatus(), equalTo(SC_ACCEPTED));
        }

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);

        final Matcher[] caseWitLAAReferenceForOffenceMatchers = Lists.newArrayList
                (withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("GR")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Granted")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("1980-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId))
                ).toArray(Matcher[]::new);

        pollProsecutionCasesProgressionFor(caseId, caseWitLAAReferenceForOffenceMatchers);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference2 = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated2 = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();

        //Receive Representation Order
        //when
        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, offenceId, "GR", laaContractNumber, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(SC_ACCEPTED));
        }

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference2);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated2);

        final Matcher[] caseWithDefendantLaaReferenceWithDrawnMatchers = Lists.newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDate", is("2019-07-15")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusCode", is("GR")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.statusDescription", is("Granted")),
                        withJsonPath("$.prosecutionCase.defendants[0].legalAidStatus", is("")),
                        withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                        withJsonPath("$.prosecutionCase.defendants[0].associationLockedByRepOrder", equalTo(true)),
                        withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", equalTo("HMP/YOI Bristol")),
                        withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.code", equalTo("C")),
                        withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description", equalTo("Custody")),
                        withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", equalTo("HMP/YOI Bristol"))).toArray(Matcher[]::new);

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

    private void resultHearingWithBailAndCustomEstablishment(final String hearingId){
        final JsonObject hearingConfirmedJson =  stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", "88cdf36e-93e4-41b0-8277-17d9dba7f06f")
                        .replaceAll("COURT_CENTRE_NAME", "Lavender Hill Magistrate Court")
        );

        final JsonObject hearingResultedJson =  stringToJsonObjectConverter.convert(
                getPayload("public.events.hearing.hearing-resulted-with-custody.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", "88cdf36e-93e4-41b0-8277-17d9dba7f06f")
                        .replaceAll("COURT_CENTRE_NAME", "Lavender Hill Magistrate Court")
        );

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata("public.listing.hearing-confirmed", userId), hearingConfirmedJson);
        JmsMessageProducerClient messageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        messageProducerClient.sendMessage("public.listing.hearing-confirmed", publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId),
                hearingResultedJson);
        messageProducerClient.sendMessage("public.events.hearing.hearing-resulted", publicEventResultedEnvelope);
    }

    private Hearing verifyHearingWithMatchers(final String hearingId, final Matcher<? super ReadContext>[] matchers) {
        final String hearingPayloadAsString = getHearingForDefendant(hearingId, matchers);
        final JsonObject hearingJsonObject = getJsonObject(hearingPayloadAsString);
        JsonObject hearingJson = hearingJsonObject.getJsonObject("hearing");
        return jsonObjectConverter.convert(hearingJson, Hearing.class);
    }

}
