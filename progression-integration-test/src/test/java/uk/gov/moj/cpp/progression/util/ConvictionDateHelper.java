package uk.gov.moj.cpp.progression.util;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.nio.charset.Charset;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvictionDateHelper extends AbstractTestHelper {

    private static final String PUBLIC_HEARING_CONVICTION_DATE_CHANGED = "public.hearing.offence-conviction-date-changed";
    private static final String PUBLIC_HEARING_CONVICTION_DATE_REMOVED = "public.hearing.offence-conviction-date-removed";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final String caseId;

    private final String offenceId;

    private final String courtApplicationId;

    public ConvictionDateHelper(final String caseId, final String offenceId, final String courtApplicationId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.courtApplicationId = courtApplicationId;

    }

    public void addConvictionDate() {
        JsonObject convictionDateChangedPayload = generateConvictionDatePayload(caseId, offenceId, PUBLIC_HEARING_CONVICTION_DATE_CHANGED, courtApplicationId);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_CONVICTION_DATE_CHANGED, randomUUID()), convictionDateChangedPayload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_CONVICTION_DATE_CHANGED, publicEventEnvelope);
    }

    public void removeConvictionDate() {
        JsonObject convictionDateRemovedPayload = generateConvictionDatePayload(caseId, offenceId, PUBLIC_HEARING_CONVICTION_DATE_REMOVED, courtApplicationId);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_CONVICTION_DATE_REMOVED, randomUUID()), convictionDateRemovedPayload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_CONVICTION_DATE_REMOVED, publicEventEnvelope);
    }

    private JsonObject generateConvictionDatePayload(String caseId, String offenceId, String eventName, final String courtApplicationId) {
        String payloadStr = getStringFromResource(eventName + ".json");
        if (caseId == null) {
            payloadStr = payloadStr.replace("\"caseId\":\"CASE_ID\",", "");
        }
        if (courtApplicationId == null) {
            payloadStr = payloadStr.replace("\"courtApplicationId\" : \"APPLICATION_ID\",", "");
        }
        if (offenceId == null) {
            payloadStr = payloadStr.replace("\"offenceId\":\"OFFENCE_ID\",", "");
            payloadStr = payloadStr.replace(",\"offenceId\":\"OFFENCE_ID\"", "");
        }
        payloadStr = payloadStr.replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID", offenceId)
                .replaceAll("APPLICATION_ID", courtApplicationId);
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private static String getStringFromResource(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }
}
