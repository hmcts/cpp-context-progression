package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class DisassociateDefenceOrganisationApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.disassociate-defence-organisation")
    public void handle(final JsonEnvelope envelope) {
        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.handler.disassociate-defence-organisation")
                .withMetadataFrom(envelope));
    }
}
