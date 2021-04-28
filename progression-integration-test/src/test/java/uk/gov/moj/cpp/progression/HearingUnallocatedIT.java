package uk.gov.moj.cpp.progression;

import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingUnallocatedIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED = "public.events.listing.hearing-unallocated";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerOffencesRemovedFromHearing = privateEvents.createConsumer("progression.events.offences-removed-from-hearing");


    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldUnallocateHearing() throws IOException {
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


        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED)
                .withUserId(userId)
                .build();

        final JsonObject hearingUnallocatedJson = getHearingMarkedAsUnallocatedObject(hearingId, offenceId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED, hearingUnallocatedJson, metadata);

        verifyInMessagingQueueForOffencesRemovedFromHearing();
        verifyHearing(hearingId);
    }

    private JsonObject createHearingAndReturnHearingId(final String prosecutionCaseId, final String defendantId, final String urn) throws IOException {
        addProsecutionCaseToCrownCourt(prosecutionCaseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(prosecutionCaseId, getProsecutionCaseMatchers(prosecutionCaseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(prosecutionCaseId)))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
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

    private static void verifyInMessagingQueueForOffencesRemovedFromHearing() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerOffencesRemovedFromHearing);
        assertTrue(message.isPresent());
    }

    private  void verifyHearing(final String hearingId) {
        poll(requestParams(getReadUrl("/hearingSearch/" + hearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearing.id", is(hearingId))
                        ));
    }
}
