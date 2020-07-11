package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class InformantRegisterApi {
    @Inject
    private Sender sender;

    @Handles("progression.add-informant-register")
    public void handleAddInformantRegister(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.add-informant-register").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("progression.generate-informant-register")
    public void handleGenerateInformantRegister(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject wrappedPayload = JsonObjects.createObjectBuilder(payload).add("registerDate", LocalDate.now().toString()).build();
        this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("progression.command.generate-informant-register").build(),
                wrappedPayload));
    }

    @Handles("progression.generate-informant-register-by-date")
    public void handleGenerateInformantRegisterByDate(final JsonEnvelope envelope) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("progression.command.generate-informant-register-by-date").build(),
                envelope.payloadAsJsonObject()));
    }
}
