package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class NotificationApi {
    @Inject
    private Sender sender;

    @Handles("progression.send.email")
    public void handleSendEmail(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(prepareMetadataForSendNotification(envelope, "progression.command.email"), envelope.payloadAsJsonObject()));
    }

    @Handles("progression.send.print")
    public void handleSendPrint(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(prepareMetadataForSendNotification(envelope, "progression.command.print"), envelope.payloadAsJsonObject()));
    }

    @Handles("progression.update-send-to-cps-flag")
    public void handleSendToCps(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(prepareMetadataForSendNotification(envelope, "progression.command.update-send-to-cps-flag"), envelope.payloadAsJsonObject()));
    }


    private Metadata prepareMetadataForSendNotification(final JsonEnvelope envelope, final String commandName) {
        return metadataFrom(envelope.metadata())
                .withName(commandName)
                .build();
    }
}
