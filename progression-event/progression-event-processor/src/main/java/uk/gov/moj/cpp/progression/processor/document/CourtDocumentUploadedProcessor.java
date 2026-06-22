package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtsDocumentUploaded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.MaterialService;

import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentUploadedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentUploadedProcessor.class.getCanonicalName());
    protected static final String PUBLIC_COURT_DOCUMENT_UPLOADED = "public.progression.events.court-document-uploaded";
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private MaterialService materialService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.court-document-uploaded")
    public void handleCourtDocumentUploadEvent(final JsonEnvelope envelope) {
        final CourtsDocumentUploaded courtsDocumentUploaded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentUploaded.class);
        final UUID fileServiceId = courtsDocumentUploaded.getFileServiceId();
        final UUID materialId = courtsDocumentUploaded.getMaterialId();
        LOGGER.info("Received progression.event.court-document-uploaded , material id {} file service id {}", materialId,fileServiceId);
        materialService.uploadMaterial(fileServiceId, materialId, envelope);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("materialId", materialId.toString())
                .build();
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_COURT_DOCUMENT_UPLOADED).apply(payload));
    }
}
