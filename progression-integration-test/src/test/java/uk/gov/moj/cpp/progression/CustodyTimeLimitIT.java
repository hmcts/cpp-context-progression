package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithTwoProsecutionCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CustodyTimeLimitIT extends AbstractIT {

    private static final String HEARING_QUERY = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_HEARING_RESULTED_WITH_CTL_EXTENSION = "public.events.hearing.hearing-resulted-with-ctl-extension";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FOR_TWO_CASE_ONE_DEFENDANT_FILE = "public.listing.hearing-confirmed-two-cases-one-defendant.json";
    private static final String PUBLIC_EVENTS_LISTING_HEARING_DELETED = "public.events.listing.hearing-deleted";


    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final JmsMessageConsumerClient prosecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient extendCustodyTimeLimitResulted = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.extend-custody-time-limit-resulted").getMessageConsumerClient();
    private static final JmsMessageConsumerClient custodyTimeLimitExtended = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.custody-time-limit-extended").getMessageConsumerClient();
    private static final JmsMessageConsumerClient custodyTimeLimitExtendedPublicEvent = newPublicJmsMessageConsumerClientProvider().withEventNames("public.events.progression.custody-time-limit-extended").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingDeleted = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-deleted").getMessageConsumerClient();;
    private static final JmsMessageConsumerClient messageConsumerOffenceDeleted = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.offences-removed-from-hearing").getMessageConsumerClient();;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String offenceId;
    private String newCourtCentreId;
    private String bailStatusCode;
    private String bailStatusDescription;
    private String bailStatusId;
    private static final String docId = randomUUID().toString();
    private static final String cpsDefendantId = randomUUID().toString();

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        bailStatusCode = "C";
        bailStatusDescription = "Remanded into Custody";
        bailStatusId = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    }

    @Test
    @Disabled("Flaky tests - passed locally failed at pipeline")
    public void shouldUpdateOffenceCustodyTimeLimitAndCpsDefendantId() throws Exception {
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        final String extendedCustodyTimeLimit = "2022-09-10";

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        verifyAddCourtDocumentForCase();

        final String initialHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        final String hearingIdForCTLExtension = randomUUID().toString();


        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_WITH_CTL_EXTENSION + ".json", caseId,
                hearingIdForCTLExtension, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final Matcher[] defendantMatchers = new Matcher[] {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", equalTo(extendedCustodyTimeLimit)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.isCtlExtended", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", equalTo(44))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantMatchers);

        final int ctlExpiryCountDown = (int) DAYS.between(LocalDate.now(), LocalDate.parse(extendedCustodyTimeLimit));
        final Matcher[] prosecutionCasesProgressionForCAAG = new Matcher[]{
                withJsonPath("$.defendants[0].id", is(defendantId)),
                withJsonPath("$.defendants[0].ctlExpiryDate", is(extendedCustodyTimeLimit)),
                withJsonPath("$.defendants[0].ctlExpiryCountDown", is(ctlExpiryCountDown)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.timeLimit", is(extendedCustodyTimeLimit)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.daysSpent", is(44))
        };

        pollProsecutionCasesProgressionForCAAG(caseId, prosecutionCasesProgressionForCAAG);

        verifyInMessagingQueueForExtendCustodyTimeLimitResulted(hearingIdForCTLExtension, caseId, offenceId, extendedCustodyTimeLimit);
        verifyInMessagingQueueForCustodyTimeLimitExtended(initialHearingId, offenceId, extendedCustodyTimeLimit);
        verifyInMessagingQueueForCustodyTimeLimitExtendedPublicEvent(initialHearingId, offenceId, extendedCustodyTimeLimit);
        verifyOffenceUpdatedWithCustodyTimeLimitAndCpsDefendantId(initialHearingId, extendedCustodyTimeLimit);

    }

    @Test
    @Disabled("Flaky tests - passed locally failed at pipeline")
    public void shouldUpdateOffenceCustodyTimeLimitForHearings() throws Exception {
        final String caseId2 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();

        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        final String extendedCustodyTimeLimit = "2022-09-10";

        // GIVEN hearing1 with case1
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        verifyAddCourtDocumentForCase();

        final String initialHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        // GIVEN hearing2 with case1 and case2
        addProsecutionCaseToCrownCourtWithTwoProsecutionCases(caseId, caseId2, defendantId, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String extendedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        doHearingConfirmedAndVerifyForTwoProsecutionCaseAndOneDefendants(extendedHearingId, caseId, caseId2, defendantId, defendantId2, courtCentreId, userId);
        doVerifyProsecutionCaseDefendantListingStatusChanged();

        // Then case1 was removed from hearing2
        final JsonObject hearingOffenceRemovedJson = getOffenceRemovedFromExistngHearingJsonObject(extendedHearingId, "3789ab16-0bb7-4ef1-87ef-c936bf0364f1");

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata("public.events.listing.offences-removed-from-allocated-hearing", userId), hearingOffenceRemovedJson);
        messageProducerClientPublic.sendMessage("public.events.listing.offences-removed-from-allocated-hearing", publicEventEnvelope);

        verifyInMessagingQueueForOffenceDeleted();

        // When hearing2 was deleted
        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(extendedHearingId);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_LISTING_HEARING_DELETED, userId), hearingDeletedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_LISTING_HEARING_DELETED, publicEventEnvelope);


        verifyInMessagingQueueForHearingDeleted();

        final String hearingIdForCTLExtension = randomUUID().toString();

        // Then Hearing1 resulted with custody time limit extended
        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_WITH_CTL_EXTENSION + ".json", caseId,
                hearingIdForCTLExtension, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final Matcher[] defendantMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", equalTo(extendedCustodyTimeLimit)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.isCtlExtended", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", equalTo(44))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantMatchers);

        final int ctlExpiryCountDown = (int) DAYS.between(LocalDate.now(), LocalDate.parse(extendedCustodyTimeLimit));
        final Matcher[] prosecutionCasesProgressionForCAAG = new Matcher[]{
                withJsonPath("$.defendants[0].id", is(defendantId)),
                withJsonPath("$.defendants[0].ctlExpiryDate", is(extendedCustodyTimeLimit)),
                withJsonPath("$.defendants[0].ctlExpiryCountDown", is(ctlExpiryCountDown)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.timeLimit", is(extendedCustodyTimeLimit)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.daysSpent", is(44))
        };

        pollProsecutionCasesProgressionForCAAG(caseId, prosecutionCasesProgressionForCAAG);

        verifyInMessagingQueueForExtendCustodyTimeLimitResulted(hearingIdForCTLExtension, caseId, offenceId, extendedCustodyTimeLimit);
        verifyInMessagingQueueForCustodyTimeLimitExtended(initialHearingId, offenceId, extendedCustodyTimeLimit);
        verifyInMessagingQueueForCustodyTimeLimitExtendedPublicEvent(initialHearingId, offenceId, extendedCustodyTimeLimit);
        verifyOffenceUpdatedWithCustodyTimeLimitAndCpsDefendantId(initialHearingId, extendedCustodyTimeLimit);

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = retrieveMessageBody(prosecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private void verifyInMessagingQueueForExtendCustodyTimeLimitResulted(final String hearingId, final String caseId, final String offenceId, final String extendedCustodyTimeLimit) {

        final Optional<JsonObject> message = retrieveMessageBody(extendCustodyTimeLimitResulted);
        assertTrue(message.isPresent());

        final JsonObject extendCustodyTimeLimitResulted = message.get();
        assertThat(extendCustodyTimeLimitResulted.getString("hearingId"), is(hearingId));
        assertThat(extendCustodyTimeLimitResulted.getString("caseId"), is(caseId));
        assertThat(extendCustodyTimeLimitResulted.getString("offenceId"), is(offenceId));
        assertThat(extendCustodyTimeLimitResulted.getString("extendedTimeLimit"), is(extendedCustodyTimeLimit));

    }

    private void verifyInMessagingQueueForCustodyTimeLimitExtended(final String hearingId, final String offenceId, final String extendedCustodyTimeLimit) {

        final Optional<JsonObject> message = retrieveMessageBody(custodyTimeLimitExtended);
        assertTrue(message.isPresent());

        final JsonObject custodyTimeLimitExtended = message.get();
        assertThat(custodyTimeLimitExtended.getJsonArray("hearingIds").getString(0), is(hearingId));
        assertThat(custodyTimeLimitExtended.getString("offenceId"), is(offenceId));
        assertThat(custodyTimeLimitExtended.getString("extendedTimeLimit"), is(extendedCustodyTimeLimit));

    }

    private void verifyInMessagingQueueForCustodyTimeLimitExtendedPublicEvent(final String hearingId, final String offenceId, final String extendedCustodyTimeLimit) {

        final Optional<JsonObject> message = retrieveMessageBody(custodyTimeLimitExtendedPublicEvent);
        assertTrue(message.isPresent());

        final JsonObject publicEvent = message.get();
        assertThat(publicEvent.getJsonArray("hearingIds").getString(0), is(hearingId));
        assertThat(publicEvent.getString("offenceId"), is(offenceId));
        assertThat(publicEvent.getString("extendedTimeLimit"), is(extendedCustodyTimeLimit));

    }

    private void verifyOffenceUpdatedWithCustodyTimeLimitAndCpsDefendantId(final String hearingId, final String extendedCustodyTimeLimit) {

        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", Matchers.is(hearingId))
        };

        final String dbHearing = pollForResponse("/hearingSearch/" + hearingId, HEARING_QUERY, hearingMatchers);
        final JsonObject hearing = stringToJsonObjectConverter.convert(dbHearing);
        final JsonObject custodyTimeLimit = hearing.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonObject("custodyTimeLimit");
        assertThat(custodyTimeLimit.getBoolean("isCtlExtended"), is(true));
        assertThat(custodyTimeLimit.getString("timeLimit"), is(extendedCustodyTimeLimit));

    }

    private void verifyAddCourtDocumentForCase() throws IOException {
        final String body = prepareAddCourtDocumentPayload();
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document-v2+json", body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", is(cpsDefendantId)))));
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document-for-case-v2.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_CPS_DEFENDANT_ID%", cpsDefendantId);

        return body;
    }

    private void doHearingConfirmedAndVerifyForTwoProsecutionCaseAndOneDefendants(String hearingId, String caseId1, String caseId2, String defendantId1, String defendantId2, String courtCentreId, String userId) {

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_FOR_TWO_CASE_ONE_DEFENDANT_FILE)
                        .replaceAll("CASE_ID_1", caseId1)
                        .replaceAll("CASE_ID_2", caseId2)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1)
                        .replaceAll("DEFENDANT_ID_2", defendantId2)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

    }

    private JsonObject getOffenceRemovedFromExistngHearingJsonObject(final String hearingId,final String offenceIdToRemove) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.hearing.selected-offences-removed-from-existing-hearing.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("OFFENCE_ID_TO_REMOVE", offenceIdToRemove)
        );
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
    }

    private void verifyInMessagingQueueForOffenceDeleted() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerOffenceDeleted);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForHearingDeleted() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingDeleted);
        assertTrue(message.isPresent());
    }
}

