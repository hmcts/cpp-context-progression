package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseCreatedInHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseCreatedInHearingEventProcessor.class.getCanonicalName());
    private static final String PUBLIC_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT = "public.events.hearing.prosecution-case-created-in-hearing";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles(PUBLIC_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT)
    public void handleProsecutionCaseCreatedInHearingPublicEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received '{}' event with payload {}", PUBLIC_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT, envelope.toObfuscatedDebugString());
        }

        sender.send(
                envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName("progression.command.create-prosecution-case-in-hearing"),
                        envelope.payload()
                )
        );

    }


}
