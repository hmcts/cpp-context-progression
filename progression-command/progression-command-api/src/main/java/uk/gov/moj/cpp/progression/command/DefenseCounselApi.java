package uk.gov.moj.cpp.progression.command;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

@ServiceComponent(COMMAND_API)
public class DefenseCounselApi {
    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.add-hearing-defence-counsel")
    public void handleAddDefenceCounsel(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.handler.add-hearing-defence-counsel");
        sender.send(commandEnvelope);

    }
    @Handles("progression.update-hearing-defence-counsel")
    public void handleUpdateDefenseCounsel(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.handler.update-hearing-defence-counsel");
        sender.send(commandEnvelope);

    }
    @Handles("progression.remove-hearing-defence-counsel")
    public void handleRemoveDefenseCounsel(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.handler.remove-hearing-defence-counsel");
        sender.send(commandEnvelope);

    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }
}