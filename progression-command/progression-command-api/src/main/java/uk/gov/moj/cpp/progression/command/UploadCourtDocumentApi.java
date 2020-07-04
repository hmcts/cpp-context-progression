package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;

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
public class UploadCourtDocumentApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private Requester requester;

    @Handles("progression.upload-court-document")
    public void handle(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.upload-court-document")
                .build();
        sender.send(envelopeFrom(metadata, envelope.payload()));
    }


    @Handles("progression.upload-court-document-for-defence")
    public void handleUploadForDefence(final JsonEnvelope envelope) {

      if(!userDetailsLoader.isPermitted(envelope, requester)) {
          throw new ForbiddenRequestException("User has neither associated or granted permission to upload");
      }


        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.upload-court-document")
                .build();
        sender.send(envelopeFrom(metadata, removeProperty(envelope.payloadAsJsonObject(), "defendantId")));
    }
}
