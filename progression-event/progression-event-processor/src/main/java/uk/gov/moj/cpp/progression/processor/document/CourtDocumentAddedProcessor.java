package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentAddedProcessor {

    public static final String PUBLIC_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    protected static final String PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED = "public.progression.idpc-document-received";
    public static final UUID IDPC_DOCUMENT_TYPE_ID = fromString("41be14e8-9df5-4b08-80b0-1e670bc80a5b");
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.court-document-added")
    public void handleCourtDocumentAddEvent(final JsonEnvelope envelope) {
        final CourtsDocumentAdded courtsDocumentAdded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentAdded.class);
        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject()).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(envelope));
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_ADDED).build();
        sender.send(envelopeFrom(metadata, envelope.payload()));

        if(courtsDocumentAdded.getCourtDocument().getDocumentTypeId().equals(IDPC_DOCUMENT_TYPE_ID)) {
            final DefendantDocument courtDocument = courtsDocumentAdded.getCourtDocument().getDocumentCategory().getDefendantDocument();
            final List<UUID> defendantIds = courtDocument.getDefendants();
            final List<Material> materials = courtsDocumentAdded.getCourtDocument().getMaterials();
            final Metadata idpcMetadata = metadataFrom(envelope.metadata())
                    .withName(PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED)
                    .build();
            defendantIds
                    .forEach(defendantId ->
                            materials
                                    .forEach(material ->
                                            sender.send(envelopeFrom(idpcMetadata,
                                                    createIDPCReceivedBody(material, courtDocument.getProsecutionCaseId(), defendantId)))));
        }
    }

    private JsonObject createIDPCReceivedBody(final Material material, final UUID caseId, UUID defendantId) {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("materialId", material.getId().toString())
                .add("defendantId", defendantId.toString())
                .add("publishedDate", material.getReceivedDateTime().toLocalDate().toString())
                .build();
    }
}





