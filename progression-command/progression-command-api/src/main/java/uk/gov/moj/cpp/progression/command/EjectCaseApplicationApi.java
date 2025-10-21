package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class EjectCaseApplicationApi {
    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.eject-case-or-application")
    public void handle(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.eject-case-or-application").apply(envelope.payloadAsJsonObject()));
    }

    @Handles("progression.eject-case-via-bdf")
    public void handleForBdf(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.eject-case-via-bdf").apply(envelope.payloadAsJsonObject()));
    }
}
