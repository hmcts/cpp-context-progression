package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class SystemDocGeneratorEventProcessor {

    private static final String DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";
    private static final String COURT_REGISTER = "CourtRegister";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles(DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailable(final JsonEnvelope documentAvailableEvent){
        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();
        final String originatingSource = documentAvailablePayload.getString("originatingSource","");
        if(COURT_REGISTER.equalsIgnoreCase(originatingSource)){
            final String fileId = documentAvailablePayload.getString("documentFileServiceId");
            final String courtCenterId = documentAvailablePayload.getString("sourceCorrelationId");
            final NotifyCourtRegister notifyCourtRegister = new NotifyCourtRegister.Builder()
                    .withCourtCentreId(UUID.fromString(courtCenterId))
                    .withSystemDocGeneratorId(UUID.fromString(fileId))
                    .build();
            this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata()).withName("progression.command.notify-court-register").build(),
                    this.objectToJsonObjectConverter.convert(notifyCourtRegister)));
        }

    }
}
