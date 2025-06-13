package uk.gov.moj.cpp.progression.command;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.service.OrganisationService;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ReceiveRepresentationOrderForDefendantApi {

    public static final String DEFENDANT_ID = "defendantId";
    public static final String OFFENCE_ID = "offenceId";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private OrganisationService organisationService;


    @Inject
    private Requester requester;

    @Handles("progression.command.receive-representationorder-for-defendant")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        validateInputs(payload);
        final String defendantId = payload.getString(DEFENDANT_ID);

        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, defendantId, requester);
        final String associatedOrganisationId = associatedOrganisation.getString("organisationId", null);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.handler.receive-representationOrder-for-defendant")
                .build();
        if (nonNull(associatedOrganisationId)) {
            sender.send(envelopeFrom(metadata, addProperty(payload, "associatedOrganisationId", associatedOrganisationId)));
        } else {
            sender.send(envelopeFrom(metadata, payload));

        }
    }

    private void validateInputs(final JsonObject payload) {
        final String defendantId = payload.containsKey(DEFENDANT_ID) ? payload.getString(DEFENDANT_ID) : null;
        final String offenceId = payload.containsKey(OFFENCE_ID) ? payload.getString(OFFENCE_ID) : null;
        final String caseId = payload.containsKey(PROSECUTION_CASE_ID) ? payload.getString(PROSECUTION_CASE_ID) : null;
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
