package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class UpdateOffencesCommandApi {
    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.update-offences-for-prosecution-case")
    public void handle(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.update-offences-for-prosecution-case");
        sender.send(commandEnvelope);
    }

    @Handles("progression.update-defendant-offences")
    public void handleUpdateDefendantOffences(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.update-defendant-offences");
        sender.send(commandEnvelope);
    }

    @Handles("progression.update-hearing-offence-plea")
    public void handleUpdatePlea(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.update-hearing-offence-plea");
        sender.send(commandEnvelope);
    }

    @Handles("progression.update-hearing-offence-verdict")
    public void handleUpdateVerdict(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.update-hearing-offence-verdict");
        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }

}
