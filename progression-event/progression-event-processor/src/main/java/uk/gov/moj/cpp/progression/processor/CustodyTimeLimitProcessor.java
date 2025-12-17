package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CustodyTimeLimitProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustodyTimeLimitProcessor.class.getName());
    private static final String PROGRESSION_STOP_CUSTODY_TIME_LIMIT_CLOCK = "progression.command.stop-custody-time-limit-clock";
    private static final String PROGRESSION_EXTEND_CUSTODY_TIME_LIMIT_CLOCK = "progression.command.extend-custody-time-limit";
    private static final String PUBLIC_EVENTS_PROGRESSION_CUSTODY_TIME_LIMIT_EXTENDED = "public.events.progression.custody-time-limit-extended";

    @Inject
    private Sender sender;

    @Handles("public.events.hearing.custody-time-limit-clock-stopped")
    public void processStopCustodyTimeLimitClock(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.events.hearing.custody-time-limit-clock-stopped event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_STOP_CUSTODY_TIME_LIMIT_CLOCK),
                jsonEnvelope.payloadAsJsonObject()));
    }

    @Handles("progression.events.extend-custody-time-limit-resulted")
    public void processExtendCustodyTimeLimitResulted(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("progression.events.extend-custody-time-limit-resulted event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_EXTEND_CUSTODY_TIME_LIMIT_CLOCK),
                jsonEnvelope.payloadAsJsonObject()));

    }

    @Handles("progression.events.custody-time-limit-extended")
    public void processCustodyTimeLimitExtended(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("progression.events.custody-time-limit-extended event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        sender.send(
                envelop(jsonEnvelope.payloadAsJsonObject())
                        .withName(PUBLIC_EVENTS_PROGRESSION_CUSTODY_TIME_LIMIT_EXTENDED)
                        .withMetadataFrom(jsonEnvelope));


    }
}

