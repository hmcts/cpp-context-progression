package uk.gov.moj.cpp.progression.command;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

@ServiceComponent(COMMAND_API)
public class PleadOnlineApi {

    @Inject
    private Sender sender;

    @Handles("progression.plead-online")
    public void handlePleadOnline(final JsonEnvelope command) {
        sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.plead-online").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("progression.plead-online-pcq-visited")
    public void handlePleadOnlinePCQVisited(final JsonEnvelope command) {
        sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.plead-online-pcq-visited").build(),
                command.payloadAsJsonObject()));
    }

}
