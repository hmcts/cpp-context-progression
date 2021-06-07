package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class UpdateDefendantCommandApi {
    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.update-defendant-for-prosecution-case")
    public void handle(final JsonEnvelope envelope) {
        JsonEnvelope commandEnvelope;
        if (envelope.payloadAsJsonObject().containsKey("matchedDefendantHearingId")) {
            commandEnvelope = envelopeWithUpdatedActionName(envelope,
                    "progression.command.update-defendant-for-matched-defendant");
        } else {
            commandEnvelope = envelopeWithUpdatedActionName(envelope,
                    "progression.command.update-defendant-for-prosecution-case");
        }

        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }

}
