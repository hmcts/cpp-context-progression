package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;


@ServiceComponent(COMMAND_API)
public class UpdateCaseMarkersApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.update-case-markers")
    public void handleUpdateCaseMarkers(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.update-case-markers").apply(envelope.payloadAsJsonObject()));
    }
}
