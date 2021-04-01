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
public class InitiateCourtApplicationProceedingsCommandApi {
    @Inject
    private Sender sender;

    @Handles("progression.initiate-court-proceedings-for-application")
    public void initiateCourtApplicationProceedings(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.initiate-court-proceedings-for-application").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("progression.edit-court-proceedings-for-application")
    public void editCourtApplicationProceedings(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.edit-court-proceedings-for-application").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("progression.add-breach-application")
    public void addBreachApplication(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.add-breach-application").build(),
                command.payloadAsJsonObject()));
    }

}
