package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class CreateCourtDocumentApi {

    @Inject
    private Sender sender;

    @Handles("progression.create-court-documents")
    public void handle(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        payload
                .getJsonArray("courtDocuments")
                .stream()
                .map(JsonObject.class::cast)
                .map(e -> createObjectBuilder().add("courtDocument", e).build())
                .forEach(e ->
                        sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                                .withName("progression.command.create-court-document")
                                .build(), e))
                );
    }

}
