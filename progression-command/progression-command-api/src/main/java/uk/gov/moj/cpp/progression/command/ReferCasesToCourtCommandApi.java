package uk.gov.moj.cpp.progression.command;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

@ServiceComponent(COMMAND_API)
public class ReferCasesToCourtCommandApi {
    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.refer-cases-to-court")
    public void handle(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.refer-cases-to-court");
        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }

}
