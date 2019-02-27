package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class UploadCourtDocumentApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.upload-court-document")
    public void handle(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.upload-court-document").apply(envelope.payloadAsJsonObject()));
    }
}
