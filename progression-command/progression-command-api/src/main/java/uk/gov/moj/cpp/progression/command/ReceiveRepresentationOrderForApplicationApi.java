package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ReceiveRepresentationOrderForApplicationApi {

    @Inject
    private Sender sender;

    @Handles("progression.command.receive-representationorder-for-application")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        validateInputs(payload);

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.handler.receive-representationOrder-for-application")
                .build();
            sender.send(envelopeFrom(metadata, envelope.payloadAsJsonObject()));
    }

    private void validateInputs(final JsonObject payload) {
        final String applicationId = payload.containsKey("applicationId") ? payload.getString("applicationId") : null;
        final String subjectId = payload.containsKey("subjectId") ? payload.getString("subjectId") : null;
        final String offenceId = payload.containsKey("offenceId") ? payload.getString("offenceId") : null;
        if (isInvalidUUID(applicationId)) {
            throw new BadRequestException("applicationId is not a valid UUID!");
        }
        if (isInvalidUUID(subjectId)) {
            throw new BadRequestException("subjectId is not a valid UUID!");
        }
        if (isInvalidUUID(offenceId)) {
            throw new BadRequestException("offenceId is not a valid UUID!");
        }
    }

    private boolean isInvalidUUID(final String string) {
        try {
            fromString(string);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
