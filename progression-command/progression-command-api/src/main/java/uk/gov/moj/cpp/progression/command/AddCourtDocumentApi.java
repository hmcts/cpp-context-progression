package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.command.api.helper.ProgressionCommandHelper.removeProperty;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class AddCourtDocumentApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.add-court-document")
    public void handle(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.add-court-document")
                .build();
        sender.send(envelopeFrom(metadata, envelope.payload()));
    }

    @Handles("progression.add-court-document-for-defence")
    public void handleAddCourtDocumentForDefence(final JsonEnvelope envelope) {

        final String defendantId = envelope.payloadAsJsonObject().getString("defendantId");
        final AddCourtDocument addCourtDocument = jsonObjectToObjectConverter.convert(envelope.asJsonObject(), AddCourtDocument.class);
        final DefendantDocument defendantDocument = addCourtDocument.getCourtDocument().getDocumentCategory().getDefendantDocument();
        if( defendantDocument!= null) {
            if(defendantDocument.getDefendants().size() != 1) {
                throw new BadRequestException("Defendant in defendant Category must be only one");
            }
            if(!defendantDocument.getDefendants().get(0).equals(fromString(defendantId))) {
                throw new BadRequestException("Defendant in the Path and body are different");
            }
        }
        if(!userDetailsLoader.isPermitted(envelope, requester)) {
            throw new ForbiddenRequestException("User has neither associated or granted permission to upload");
        }

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.add-court-document")
                .build();
        sender.send(envelopeFrom(metadata, removeProperty(envelope.payloadAsJsonObject(), "defendantId")));

    }
}
