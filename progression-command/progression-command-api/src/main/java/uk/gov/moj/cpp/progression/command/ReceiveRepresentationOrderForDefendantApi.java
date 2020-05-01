package uk.gov.moj.cpp.progression.command;

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

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.command.api.helper.ProgressionCommandHelper.addProperty;

@ServiceComponent(COMMAND_API)
public class ReceiveRepresentationOrderForDefendantApi {

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
        final String defendantId = payload.getString("defendantId");
        final JsonObject associatedOrganisation = organisationService.getAssociatedOrganisation(envelope, defendantId, requester);
        final String associatedOrganisationId = associatedOrganisation.getString("organisationId", null);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.handler.receive-representationOrder-for-defendant")
                .build();
        if(nonNull(associatedOrganisationId)) {
            sender.send(envelopeFrom(metadata, addProperty(payload, "associatedOrganisationId", associatedOrganisationId)));
        } else {
            sender.send(envelopeFrom(metadata, payload));

        }
    }
}
