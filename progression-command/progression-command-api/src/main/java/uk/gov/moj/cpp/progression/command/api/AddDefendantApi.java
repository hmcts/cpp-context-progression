package uk.gov.moj.cpp.progression.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import javax.inject.Inject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ServiceComponent(COMMAND_API)
public class AddDefendantApi {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.add-defendant")
    public void addDefendant(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.record-add-defendant");
        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }
}
