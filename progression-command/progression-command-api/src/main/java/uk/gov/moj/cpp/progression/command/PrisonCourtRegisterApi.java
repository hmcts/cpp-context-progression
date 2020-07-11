package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class PrisonCourtRegisterApi {

    @Inject
    private Sender sender;

    @Handles("progression.add-prison-court-register")
    public void handleAddPrisonCourtRegister(final JsonEnvelope command) {
        sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.add-prison-court-register").build(),
                command.payloadAsJsonObject()));
    }
}