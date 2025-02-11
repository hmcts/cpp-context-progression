package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

public class HearingUnallocatedIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED = "public.events.listing.hearing-unallocated";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @Test
    public void shouldUnallocateHearing() throws IOException, JSONException {
        final String userId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String urn = generateUrn();
        final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1"; // sourced directly from payload file
        final String hearingId = createHearingAndReturnHearingId(prosecutionCaseId, defendantId, urn);
        getHearingForDefendant(hearingId, new Matcher[]{withJsonPath("$.hearing.prosecutionCases", hasSize(1))});

        final JsonObject hearingUnallocatedJson = getHearingMarkedAsUnallocatedObject(hearingId, offenceId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED, userId), hearingUnallocatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED, publicEventEnvelope);
        getHearingForDefendant(hearingId, new Matcher[]{withJsonPath("$.hearing.prosecutionCases", hasSize(0))});
    }

    private String createHearingAndReturnHearingId(final String prosecutionCaseId, final String defendantId, final String urn) throws IOException, JSONException {
        addProsecutionCaseToCrownCourt(prosecutionCaseId, defendantId, urn);
        return pollCaseAndGetHearingForDefendant(prosecutionCaseId, defendantId);
    }

    private JsonObject getHearingMarkedAsUnallocatedObject(final String hearingId, final String offenceId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-unallocated.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("OFFENCE_ID", offenceId)
        );
    }
}
