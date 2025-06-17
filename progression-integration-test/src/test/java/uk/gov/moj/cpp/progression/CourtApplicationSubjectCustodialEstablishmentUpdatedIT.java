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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplicationWithRespondents;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
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
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class CourtApplicationSubjectCustodialEstablishmentUpdatedIT extends AbstractIT {

    public static final String PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON = "application/vnd.progression.query.prosecutioncase.caag+json";
    public static final String PROGRESSION_QUERY_APPLICATION_AAAG_JSON = "application/vnd.progression.query.application.aaag+json";


    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private String hearingId;
    private String defendantId;
    private ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String courtApplicationId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        hearingId = randomUUID().toString();
        courtApplicationId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldUpdateDefendantsDetails_WithNonEmptyCustodyEstablishment_WithEmptyCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();

        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // initiation of  application
        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.initiate-court-proceedings-for-application.json");
        pollForApplication(courtApplicationId);

        helper.updateDefendantWithCustodyEstablishmentInfo(caseId, defendantId, masterDefendantId);

        final Matcher[] defendantWithCustodialEstablishmentMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollProsecutionCasesProgressionFor(caseId, defendantWithCustodialEstablishmentMatchers);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplication = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplication);

        helper.updateDefendantWithEmptyCustodyEstablishmentInfo(caseId, defendantId, masterDefendantId);

        final Matcher[] matchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody"),
        };

        pollProsecutionCasesProgressionFor(caseId, matchers);

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody"),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment);
    }

    @Test
    public void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddress() throws Exception {
        final String masterDefendantId = defendantId;
        String defendantId1 = randomUUID().toString();
        String defendantId2 = randomUUID().toString();

        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // initiation of  application
        intiateCourtProceedingForApplicationWithRespondents(defendantId1, defendantId2, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(defendantId1, defendantId2, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        pollForApplicationAtAGlance(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    @Test
    void shouldUpdateDefendantsDetailsWhenNewApplicantIsCreatedWithUpdatedAddressForCourtOrder() throws Exception {
        final String masterDefendantId = defendantId;
        String id2 = randomUUID().toString();
        String id3 = randomUUID().toString();

        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        pollProsecutionCasesProgressionForCAAG(caseId, withJsonPath("$", not(emptyOrNullString())));

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // initiation of  application
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId, caseId, defendantId, masterDefendantId, "Address1", hearingId,
                "applications/progression.initiate-court-proceedings-for-court-order-linked-application-for-updated-address.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address1"))));

        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address1"))));

        // initiation of  other application
        String courtApplicationId1 = randomUUID().toString();
        intiateCourtProceedingForApplicationWithRespondents(id2, id3, courtApplicationId1, caseId, defendantId, masterDefendantId, "Address2", hearingId,
                "applications/progression.initiate-court-proceedings-for-application-with-respondents.json");

        pollProsecutionCasesProgressionForCAAG(caseId, anyOf(
                withJsonPath("$.defendants[0].address.address1", is("sam2Address2Address2"))));

        pollForApplicationAtAGlance(courtApplicationId1, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))));
        pollForApplicationAtAGlance(courtApplicationId, anyOf(
                withJsonPath("$.respondentDetails[1].address.address1", is("sam2Address2Address2"))
        ));
    }

    @Test
    public void shouldUpdateDefendantsDetails_WithNonEmptyCustodyEstablishment_WithCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();

        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        // initiation of  case
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));


        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.initiate-court-proceedings-for-application-with-custodial-establishment.json");

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.name"),
                withoutJsonPath("$.courtApplication.subject.masterDefendant.personDefendant.custodialEstablishment.custody"),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment);


    }

    @Test
    void shouldUpdateDefendantsDetails_WithEmptyCustodyEstablishment_WithNonEmptyCustodyEstablishment() throws Exception {

        final String masterDefendantId = randomUUID().toString();

        // initiation of  case
        final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        initiateCourtProceedingsForMatchedDefendants(caseId, defendantId, masterDefendantId);
        verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);

        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());
        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        // initiation of  application
        intiateCourtProceedingForApplication(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.initiate-court-proceedings-for-application-with-judicial-result-custodial-establishment.json");
        sendHearingResultedPayload(jsonFromString(getCourtApplicationJson3(courtApplicationId, caseId, defendantId, masterDefendantId, hearingId, "applications/progression.event.hearing-resulted.json")));

        final Matcher[] custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.respondents[0].masterDefendant.personDefendant.custodialEstablishment.name", is("HMP Birmingham")),
                withJsonPath("$.courtApplication.respondents[0].masterDefendant.personDefendant.custodialEstablishment.custody", is("Prison")),
        };

        pollForApplication(courtApplicationId, custodyEstablishmentDefendantUpdatedMatchersForApplicationMatcherEmptyCustodyEstablishment);

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
