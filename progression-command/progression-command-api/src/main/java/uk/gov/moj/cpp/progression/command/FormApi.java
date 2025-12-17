package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class FormApi {

    @Inject
    private Sender sender;

    @Handles("progression.create-form")
    public void createForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.create-form")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.update-form")
    public void updateForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.update-form")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.finalise-form")
    public void finaliseForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.finalise-form")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.update-form-defendants")
    public void updateBcmDefendants(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.update-form-defendants")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.request-edit-form")
    public void requestEditForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.request-edit-form")
                .withMetadataFrom(envelope));
    }

}
