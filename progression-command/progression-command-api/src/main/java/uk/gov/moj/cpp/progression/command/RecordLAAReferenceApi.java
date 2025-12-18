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
public class RecordLAAReferenceApi {

    public static final String DEFENDANT_ID = "defendantId";
    public static final String OFFENCE_ID = "offenceId";
    public static final String CASE_ID = "caseId";
    public static final String SUBJECT_ID = "subjectId";
    public static final String APPLICATION_ID = "applicationId";

    @Inject
    private Sender sender;


    @Handles("progression.command.record-laareference-for-offence")
    public void handle(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        validateInputsForCase(payload);

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.handler.record-laareference-for-offence")
                .build();
        sender.send(envelopeFrom(metadata, payload));
    }

    @Handles("progression.command.record-laareference-for-application")
    public void handleForApplication(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        validateInputsForApplication(payload);

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.handler.record-laareference-for-application")
                .build();
        sender.send(envelopeFrom(metadata, payload));
    }

    private void validateInputsForApplication(final JsonObject payload) {
        final String applicationId = payload.containsKey(APPLICATION_ID) ? payload.getString(APPLICATION_ID) : null;
        final String subjectId = payload.containsKey(SUBJECT_ID) ? payload.getString(SUBJECT_ID) : null;
        final String offenceId = payload.containsKey(OFFENCE_ID) ? payload.getString(OFFENCE_ID) : null;
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

    private void validateInputsForCase(final JsonObject payload) {
        final String defendantId = payload.containsKey(DEFENDANT_ID) ? payload.getString(DEFENDANT_ID) : null;
        final String offenceId = payload.containsKey(OFFENCE_ID) ? payload.getString(OFFENCE_ID) : null;
        final String caseId = payload.containsKey(CASE_ID) ? payload.getString(CASE_ID) : null;
        if (isInvalidUUID(caseId)) {
            throw new BadRequestException("caseId is not a valid UUID!");
        }
        if (isInvalidUUID(defendantId)) {
            throw new BadRequestException("defendantId is not a valid UUID!");
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
