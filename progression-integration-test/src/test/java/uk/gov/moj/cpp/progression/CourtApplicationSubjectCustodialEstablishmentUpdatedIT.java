package uk.gov.moj.cpp.progression;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplicationWithRespondents;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_APPLICATION_AAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyHearingApplicationLinkCreated;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyHearingInMessagingQueueForReferToCourt;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CourtApplicationSubjectCustodialEstablishmentUpdatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private String hearingId;
    private String defendantId;
    ProsecutionCaseUpdateDefendantHelper helper;
    private String courtCentreId;
    private String caseId;
    private String courtApplicationId;
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application.json";

    private static final JmsMessageConsumerClient privateEventConsumerForpUpdateDefendantAddressOnCase = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-defendant-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient hearingApplicationLinkCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-application-link-created").getMessageConsumerClient();
    private static final JmsMessageConsumerClient applicationReferralToExistingHearingMessageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.application-referral-to-existing-hearing").getMessageConsumerClient();

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        stubInitiateHearing();
        hearingId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = UUID.randomUUID().toString();
    }


    @Test
    public void shouldUpdateDefendantsDetails_WithNonEmptyCustodyEstablishment_WithEmptyCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();

        // initiation of  case
        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        // initiation of  application
        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.initiate-court-proceedings-for-application.json");
        verifyHearingInMessagingQueueForReferToCourt(applicationReferralToExistingHearingMessageConsumer);
        verifyHearingApplicationLinkCreated(hearingId, hearingApplicationLinkCreated);

        helper.updateDefendantWithCustodyEstablishmentInfo(caseId, defendantId, masterDefendantId);

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        pollProsecutionCasesProgressionFor(caseId, custodyEstablishmentDefendantUpdatedMatchers);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplication = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplication);

        helper.updateDefendantWithEmptyCustodyEstablishmentInfo(caseId, defendantId, masterDefendantId);


        final Matcher[] defendantUpdatedMatchersEmptyCustodyEstablishment = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment = new Matcher[]{
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchersEmptyCustodyEstablishment);
        pollProsecutionCasesProgressionFor(caseId, custodyEstablishmentDefendantUpdatedMatchersEmptyCustodyEstablishment);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplicationMatecherEmptyCustodyEstablishment = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody"),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplicationMatecherEmptyCustodyEstablishment);


    }

    @Test
    public void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddress() throws Exception {
        final String masterDefendantId = defendantId;
        String id2 = randomUUID().toString();
        String id3 = randomUUID().toString();

        // initiation of  case
        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();

        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        String res = getProgressionCaseHearings(caseId);
        assertFalse(res.isEmpty());

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        // initiation of  application
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");
        verifyHearingInMessagingQueueForReferToCourt(applicationReferralToExistingHearingMessageConsumer);
        verifyHearingApplicationLinkCreated(hearingId, hearingApplicationLinkCreated);

        Optional<JsonObject> message = retrieveMessageBody(privateEventConsumerForpUpdateDefendantAddressOnCase);
        assertTrue(message.isPresent());

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");
        verifyHearingInMessagingQueueForReferToCourt(applicationReferralToExistingHearingMessageConsumer);
        verifyHearingApplicationLinkCreated(hearingId, hearingApplicationLinkCreated);

        message = retrieveMessageBody(privateEventConsumerForpUpdateDefendantAddressOnCase);
        assertTrue(message.isPresent());

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    @Test
    void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddressForCourtOrder() throws Exception {
        final String masterDefendantId = defendantId;
        String id2 = randomUUID().toString();
        String id3 = randomUUID().toString();

        // initiation of  case
        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();

        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        String res = getProgressionCaseHearings(caseId);
        assertFalse(res.isEmpty());

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        // initiation of  application
        final Response response = intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-court-order-linked-application-for-updated-address.json");
        verifyHearingInMessagingQueueForReferToCourt(applicationReferralToExistingHearingMessageConsumer);
        verifyHearingApplicationLinkCreated(hearingId, hearingApplicationLinkCreated);

        Optional<JsonObject> message = retrieveMessageBody(privateEventConsumerForpUpdateDefendantAddressOnCase);
        assertTrue(message.isPresent());

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");
        verifyHearingInMessagingQueueForReferToCourt(applicationReferralToExistingHearingMessageConsumer);
        verifyHearingApplicationLinkCreated(hearingId, hearingApplicationLinkCreated);

        message = retrieveMessageBody(privateEventConsumerForpUpdateDefendantAddressOnCase);
        assertTrue(message.isPresent());

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        getProgressionQueryForApplicationAtAaag(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        getProgressionQueryForApplicationAtAaag(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    @Test
    void shouldUpdateDefendantsDetails_WithNonEmptyCustodyEstablishment_WithCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();

        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));


        final JmsMessageConsumerClient privateEventConsumerForDefendantCustodialEstablishmentRemoved = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.defendant-custodial-establishment-removed").getMessageConsumerClient();
        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.initiate-court-proceedings-for-application-with-custodial-establishment.json");
        verifyHearingInMessagingQueueForReferToCourt(applicationReferralToExistingHearingMessageConsumer);
        verifyHearingApplicationLinkCreated(hearingId, hearingApplicationLinkCreated);
        verifyInMessagingQueueForRemoveDefendantCustodialEstablishmentFromCaseRequested(privateEventConsumerForDefendantCustodialEstablishmentRemoved);


    }

    private void verifyInMessagingQueueForProsecutionCaseCreated(final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }

    private void verifyInMessagingQueueForRemoveDefendantCustodialEstablishmentFromCaseRequested(final JmsMessageConsumerClient privateEventConsumerForRemoveDefendantCustodialEstablishmentFromCaseRequested) {
        final Optional<JsonObject> message = retrieveMessageBody(privateEventConsumerForRemoveDefendantCustodialEstablishmentFromCaseRequested);
        assertTrue(message.isPresent());
    }

    private void sendHearingResultedPayload(final JsonObject publicHearingResultedJsonObject) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, AbstractTestHelper.USER_ID), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventEnvelope);
    }

    public static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }


    private static String getCourtApplicationJson3(final String applicationId, final String caseId, final String defendantId, final String masterDefendantId, final String hearingId, final String fileName) throws IOException {
        String payloadJson;
        payloadJson = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("MASTERDEFENDANTID", masterDefendantId)
                .replaceAll("HEARING_ID", hearingId);
        return payloadJson;
    }

    private static String getProgressionCaseHearings(final String caseId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl("/prosecutioncases/" + caseId),
                PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private static String getProgressionQueryForApplicationAtAaag(final String applicationId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl("/applications/" + applicationId),
                PROGRESSION_QUERY_APPLICATION_AAAG_JSON)
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }



}
