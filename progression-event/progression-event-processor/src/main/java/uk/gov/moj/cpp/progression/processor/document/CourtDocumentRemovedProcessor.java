package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentRemovedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentRemovedProcessor.class.getCanonicalName());
    protected static final String PUBLIC_PROGRESSION_EVENTS_COURT_DOCUMENT_REMOVED = "public.progression.events.court-document-removed";
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.court-document-removed")
    public void handleCourtDocumentRemovedEvent(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received progression.event.court-document-removed , payload {} ", envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_PROGRESSION_EVENTS_COURT_DOCUMENT_REMOVED).apply(envelope.payloadAsJsonObject()));
    }
}
