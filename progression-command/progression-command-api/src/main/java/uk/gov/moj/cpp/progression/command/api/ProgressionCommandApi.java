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
public class ProgressionCommandApi {
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;


    @Handles("progression.command.add-case-to-crown-court")
    public void addCaseToCrownCourt(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope,
                "progression.command.handler.add-case-to-crown-court");
        sender.send(commandEnvelope);
    }

    @Handles("progression.command.sending-committal-hearing-information")
    public void sendCommittalHearingInformation(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.handler.sending-committal-hearing-information");
        sender.send(commandEnvelope);
    }

    @Handles("progression.command.sentence-hearing-date")
    public void addSentenceHearingDate(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.record-sentence-hearing-date");
        sender.send(commandEnvelope);
    }


    @Handles("progression.command.request-psr-for-defendants")
    public void requestPSRForDefendants(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.handler.request-psr-for-defendants");
        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }

    @Handles("progression.command.complete-sending-sheet")
    public void completeSendingSheet(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
