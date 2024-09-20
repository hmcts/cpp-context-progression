package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingUnallocatedIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED = "public.events.listing.hearing-unallocated";


    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerOffencesRemovedFromHearing = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.offences-removed-from-hearing").getMessageConsumerClient();


    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @Test
    public void shouldUnallocateHearing() throws IOException, JSONException {
        final String userId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String urn = generateUrn();
        final JsonObject jsonObject = createHearingAndReturnHearingId(prosecutionCaseId, defendantId, urn);
        final String hearingId = jsonObject.getJsonObject("hearing").getString("id");
        final String offenceId = jsonObject.getJsonObject("hearing")
                .getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getString("id");


        final JsonObject hearingUnallocatedJson = getHearingMarkedAsUnallocatedObject(hearingId, offenceId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED, userId), hearingUnallocatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED, publicEventEnvelope);

        verifyInMessagingQueueForOffencesRemovedFromHearing();
        verifyHearing(hearingId);
    }

    private JsonObject createHearingAndReturnHearingId(final String prosecutionCaseId, final String defendantId, final String urn) throws IOException, JSONException {
        addProsecutionCaseToCrownCourt(prosecutionCaseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(prosecutionCaseId, getProsecutionCaseMatchers(prosecutionCaseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(prosecutionCaseId)))));

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged;
    }

    private JsonObject getHearingMarkedAsUnallocatedObject(final String hearingId, final String offenceId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-unallocated.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("OFFENCE_ID", offenceId)
        );
    }

    private void verifyInMessagingQueueForOffencesRemovedFromHearing() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerOffencesRemovedFromHearing);
        assertTrue(message.isPresent());
    }

    private void verifyHearing(final String hearingId) {
        poll(requestParams(getReadUrl("/hearingSearch/" + hearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearing.id", is(hearingId))
                        ));
    }
}
