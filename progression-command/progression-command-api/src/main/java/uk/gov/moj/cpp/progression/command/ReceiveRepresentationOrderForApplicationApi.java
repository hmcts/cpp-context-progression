package uk.gov.moj.cpp.progression.command;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Stream.concat;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.command.helper.LAAHelper.isInvalidUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.helper.LAAHelper;

import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ReceiveRepresentationOrderForApplicationApi {

    private static final String APPLICATION_ID = "applicationId";
    public static final String PROGRESSION_COMMAND_HANDLER_RECEIVE_REPRESENTATION_ORDER_FOR_APPLICATION_ON_APPLICATION = "progression.command.handler.receive-representationOrder-for-application-on-application";
    public static final String SUBJECT_ID = "subjectId";
    public static final String OFFENCE_ID = "offenceId";

    @Inject
    private Sender sender;

    @Inject
    private LAAHelper laaHelper;

    @Handles("progression.command.receive-representationorder-for-application")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        validateInputs(envelope, payload);

        final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
        final List<UUID> childApplicationIds = laaHelper.getChildApplicationList(applicationId);


        sender.send(envelopeFrom(getMetadata(envelope, "progression.command.handler.receive-representationOrder-for-application"), payload));

        childApplicationIds.forEach(appId -> {
            JsonObject updatedPayload = createObjectBuilder(payload)
                    .add(APPLICATION_ID, appId.toString())  // override with current id
                    .remove(SUBJECT_ID) // remove subjectId and offenceId as laa reference should be attached on application level for child
                    .remove(OFFENCE_ID)
                    .build();

            sender.send(envelopeFrom(getMetadata(envelope, PROGRESSION_COMMAND_HANDLER_RECEIVE_REPRESENTATION_ORDER_FOR_APPLICATION_ON_APPLICATION), updatedPayload));
        });

    }

    private static Metadata getMetadata(final JsonEnvelope envelope, final String name) {
        return metadataFrom(envelope.metadata())
                .withName(name)
                .build();
    }

    @Handles("progression.command.receive-representationorder-for-application-on-application")
    public void handleForApplicationOnApplication(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String applicationIdString = payload.getString(APPLICATION_ID, null);

        laaHelper.validateInputsForApplication(applicationIdString, envelope);
        final UUID applicationId = fromString(applicationIdString);

        final List<UUID> childApplicationIds = laaHelper.getChildApplicationList(applicationId);

        //add parent application id to the list
        final List<UUID> finalApplicationIdList = concat(Stream.of(applicationId), childApplicationIds.stream()).toList();

        final Metadata metadata = getMetadata(envelope, PROGRESSION_COMMAND_HANDLER_RECEIVE_REPRESENTATION_ORDER_FOR_APPLICATION_ON_APPLICATION);

        finalApplicationIdList.forEach(appId -> {
            JsonObject updatedPayload = createObjectBuilder(payload)
                    .add(APPLICATION_ID, appId.toString())  // override with current id
                    .build();

            sender.send(envelopeFrom(metadata, updatedPayload));
        });

    }

    private void validateInputs(final JsonEnvelope envelope, final JsonObject payload) {
        final String applicationId = payload.getString(APPLICATION_ID, null);
        final String subjectId = payload.getString(SUBJECT_ID, null);
        final String offenceId = payload.getString(OFFENCE_ID, null);
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

}
