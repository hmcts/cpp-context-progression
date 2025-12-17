package uk.gov.moj.cpp.progression.command;


import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.helper.LAAHelper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    @Inject
    private LAAHelper laaHelper;

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
        validateInputsForApplication(envelope, payload);

        final String applicationId = payload.getString(APPLICATION_ID, null);
        final List<UUID> applicationIds = laaHelper.getChildApplicationList(fromString(applicationId));

        final Metadata metadata = getMetadata(envelope, "progression.command.handler.record-laareference-for-application");

        final Metadata metadataForChildApplications =
                getMetadata(envelope, "progression.command.handler.record-laareference-for-application-on-application");

        applicationIds.forEach(appId -> {
            JsonObject updatedPayload = createObjectBuilder(payload)
                    .add(APPLICATION_ID, appId.toString())
                    .remove(SUBJECT_ID)// override with current id
                    .remove(OFFENCE_ID)// override with current id
                    .build();
            sender.send(envelopeFrom(metadataForChildApplications, updatedPayload));
        });

        sender.send(envelopeFrom(metadata, payload));
    }

    private static Metadata getMetadata(final JsonEnvelope envelope, final String name) {
        return metadataFrom(envelope.metadata())
                .withName(name)
                .build();
    }

    @Handles("progression.command.record-laareference-for-application-on-application")
    public void handleForApplicationOnApplication(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String applicationId = payload.getString(APPLICATION_ID, null);

        laaHelper.validateInputsForApplication(applicationId, envelope);
        final List<UUID> childApplicationIds = laaHelper.getChildApplicationList(fromString(applicationId));

        // append child app Ids to applicationId
        final List<UUID> finalApplicationIdList = Stream.concat(Stream.of(UUID.fromString(applicationId)), childApplicationIds.stream()).toList();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.handler.record-laareference-for-application-on-application")
                .build();

        finalApplicationIdList.forEach(appId -> {
            JsonObject updatedPayload = createObjectBuilder(payload)
                    .add(APPLICATION_ID, appId.toString())  // override with current id
                    .build();
            sender.send(envelopeFrom(metadata, updatedPayload));
        });

    }

    private void validateInputsForApplication(final JsonEnvelope envelope, final JsonObject payload) {
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
        final CourtApplication courtApplication = laaHelper.getCourtApplication(envelope, fromString(applicationId));
        if (nonNull(courtApplication.getCourtApplicationCases())) {
            boolean isOffencesAvailableInApplication = courtApplication.getCourtApplicationCases().stream()
                    .anyMatch(courtApplicationCase -> nonNull(courtApplicationCase.getOffences()));
            if (!isOffencesAvailableInApplication) {
                throw new BadRequestException("No Offences found for application id: " + applicationId);
            }
        }else{
            throw new BadRequestException("No linked case found for application id: " + applicationId);
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
