package uk.gov.moj.cpp.progression.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.command.api.service.StructureReadService;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ProgressionCommandApi {
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private StructureReadService structureCaseService;


    @Handles("progression.command.add-case-to-crown-court")
    public void addCaseToCrownCourt(final JsonEnvelope envelope) {
        String userId = envelope.metadata().userId()
                .orElseThrow(() -> new RuntimeException("User Id not found in metadata"));
        List<String> defendentdIdsForCase = structureCaseService.getStructureCaseDefendantsId(
                envelope.payloadAsJsonObject().getString("caseId"), userId);
        JsonArrayBuilder defendantsBuilder = Json.createArrayBuilder();

        defendentdIdsForCase.forEach(
                s -> defendantsBuilder.add(Json.createObjectBuilder().add("id", s)));

        final JsonObject command = JsonObjects.createObjectBuilder(envelope.payloadAsJsonObject())
                .add("defendants", defendantsBuilder.build()).build();

        JsonEnvelope modifiedJsonEnvelope = enveloper
                .withMetadataFrom(envelope, "progression.command.add-case-to-progression")
                .apply(command);
        sender.send(modifiedJsonEnvelope);
    }

    @Handles("progression.command.sending-committal-hearing-information")
    public void sendCommittalHearingInformation(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
    
    @Handles("progression.command.sentence-hearing-date")
    public void addSentenceHearingDate(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.record-sentence-hearing-date");
        sender.send(commandEnvelope);
    }

    @Handles("progression.command.case-to-be-assigned")
    public void updateCaseToBeAssigned(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.case-assigned-for-review")
    public void updateCaseAssignedForReview(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.prepare-for-sentence-hearing")
    public void prepareForSentenceHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.add-defendant-additional-information")
    public void addAdditionalInformationForDefendant(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.handler.add-defendant-additional-information");
        sender.send(commandEnvelope);
    }

    @Handles("progression.command.no-more-information-required")
    public void noMoreInformationRequired(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.record-no-more-information-required");
        sender.send(commandEnvelope);
    }

    @Handles("progression.command.request-psr-for-defendants")
    public void requestPSRForDefendants(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.add-sentence-hearing")
    public void addSentenceHearing(final JsonEnvelope envelope) {
        final JsonEnvelope commandEnvelope = envelopeWithUpdatedActionName(envelope, "progression.command.record-sentence-hearing");
        sender.send(commandEnvelope);
    }

    private JsonEnvelope envelopeWithUpdatedActionName(final JsonEnvelope existingEnvelope, final String name) {
        return enveloper.withMetadataFrom(existingEnvelope, name).apply(existingEnvelope.payloadAsJsonObject());
    }
}
