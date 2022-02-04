package uk.gov.moj.cpp.progression.command;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@ServiceComponent(COMMAND_API)
public class PetFormApi {

    @Inject
    private Sender sender;

    @Handles("progression.create-pet-form")
    public void createPetForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.create-pet-form")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.update-pet-form")
    public void updatePetForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.update-pet-form")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.finalise-pet-form")
    public void finalisePetForm(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.finalise-pet-form")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.update-pet-detail")
    public void updatePetDetail(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.update-pet-detail")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.update-pet-form-for-defendant")
    public void updatePetFormForDefendant(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.update-pet-form-for-defendant")
                .withMetadataFrom(envelope));
    }

}
