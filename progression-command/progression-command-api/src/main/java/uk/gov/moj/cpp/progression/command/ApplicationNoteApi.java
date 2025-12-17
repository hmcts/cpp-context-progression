package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class ApplicationNoteApi {

    @Inject
    private Sender sender;

    @Handles("progression.command.add-application-note")
    public void addApplicationNote(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName("progression.command.handler.add-application-note"),
                envelope.payload()));
    }

    @Handles("progression.command.edit-application-note")
    public void editApplicationNote(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName("progression.command.handler.edit-application-note"),
                envelope.payload()));
    }

}
