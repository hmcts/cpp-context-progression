package uk.gov.moj.cpp.progression.command;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

@ServiceComponent(COMMAND_API)
public class ReceiveRepresentationOrderForDefendantApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.command.receive-representationorder-for-defendant")
    public void handle(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.handler.receive-representationOrder-for-defendant").apply(envelope.payloadAsJsonObject()));
    }
}
