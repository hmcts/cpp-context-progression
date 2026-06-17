package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtsDocumentRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.client.MaterialClient;

import java.util.UUID;

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

    @Inject
    MaterialClient materialClient;

    @Handles("progression.event.court-document-removed")
    public void handleCourtDocumentRemovedEvent(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received progression.event.court-document-removed , payload {} ", envelope.toObfuscatedDebugString());
        }
        final CourtsDocumentRemoved courtsDocumentRemoved = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentRemoved.class);
        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalArgumentException("No UserId Supplied"));
        materialClient.removeMaterial(courtsDocumentRemoved.getMaterialId(), UUID.fromString(userId), createObjectBuilder().build());
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_PROGRESSION_EVENTS_COURT_DOCUMENT_REMOVED).apply(envelope.payloadAsJsonObject()));
    }
}
