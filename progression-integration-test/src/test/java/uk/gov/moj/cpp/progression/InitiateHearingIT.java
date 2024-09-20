package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class InitiateHearingIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final JmsMessageConsumerClient publicEventsConsumerForOffencesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-offences-changed").getMessageConsumerClient();
    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-offences-updated").getMessageConsumerClient();

    private static final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-cases-referred-to-court").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    public static final String PROGRESSION_QUERY_GET_CASE_HEARING_TYPES = "application/vnd.progression.query.case.hearingtypes+json";

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @Test
    public void shouldInitiateHearing() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))));

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");

        getHearingForDefendant(hearingId, new Matcher[0]);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject(caseId, hearingId, defendantId, courtCentreId);


        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

        verifyInMessagingQueueForCasesReferredToCourts();

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endorsableFlag", equalTo(true)));

        verifyCaseHearingTypes(caseId, LocalDate.now());
        UUID offenceId = randomUUID();
        ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId.toString());        // when
        helper.updateOffences();

        // then
        helper.verifyInActiveMQ(privateEventsConsumer);
        helper.verifyInMessagingQueueForOffencesUpdated(publicEventsConsumerForOffencesUpdated);
    }

    private static void sendPublicEvent(final String eventName, final Metadata metadata, final JsonObject hearingConfirmedJson) {
        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(metadata, hearingConfirmedJson));
    }

    @Test
    public void shouldInitiateHearing_ShouldSendNotifications() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.randomUUID().toString();

        final JmsMessageConsumerClient messageConsumerEmailRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPrintRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.print-requested").getMessageConsumerClient();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps-no-email.json", randomUUID());

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))));

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");

        getHearingForDefendant(hearingId, new Matcher[0]);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObjectWithNotificationFlagTrue(caseId, hearingId, defendantId, courtCentreId);

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

        verifyInMessagingQueueForCasesReferredToCourts();

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endorsableFlag", equalTo(true)));

        verifyCaseHearingTypes(caseId, LocalDate.now());
        UUID offenceId = randomUUID();
        ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId.toString());        // when
        helper.updateOffences();

        // then
        helper.verifyInActiveMQ(privateEventsConsumer);
        helper.verifyInMessagingQueueForOffencesUpdated(publicEventsConsumerForOffencesUpdated);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerEmailRequestPrivateEvent, caseId);
        doVerifyListHearingRequestedPrivateEvent(messageConsumerPrintRequestPrivateEvent, caseId);
    }

    private void doVerifyListHearingRequestedPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message.get(), Matchers.notNullValue());
        final JsonObject progressionCommandNotificationEvent = message.get();
        assertThat(progressionCommandNotificationEvent.getString("caseId", EMPTY), Matchers.is(caseId));
    }

    private JsonObject getHearingJsonObject(final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingJsonObjectWithNotificationFlagTrue(final String caseId, final String hearingId,
                                                                    final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.listing.hearing-confirmed-notification-true.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private void verifyInMessagingQueueForCasesReferredToCourts() {
        Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private static void verifyCaseHearingTypes(final String caseId, final LocalDate orderDate) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId + "?orderDate=" + orderDate.toString()), PROGRESSION_QUERY_GET_CASE_HEARING_TYPES)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearingTypes.length()", is(1)),
                                withJsonPath("$.hearingTypes[0].hearingId", is(notNullValue())),
                                withJsonPath("$.hearingTypes[0].type", is(notNullValue()))
                        )));
    }
}

