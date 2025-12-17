package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;


import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class UpdateDefendantListingStatusCommandApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.update-defendant-listing-status")
    public void handle(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.update-defendant-listing-status")
                .build();

        sender.send(envelopeFrom(metadata,removeProperty(envelope.payloadAsJsonObject(), "hearingId")));

    }

    @Handles("progression.update-listing-number")
    public void handleUpdateListingNumbers(final JsonEnvelope envelope) {

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.update-listing-number")
                .build();

        sender.send(envelopeFrom(metadata,envelope.payloadAsJsonObject()));

    }

    @Handles("progression.update-index-for-bdf")
    public void handleUpdateIndex(final JsonEnvelope envelope) {

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.update-index-for-bdf")
                .build();

        sender.send(envelopeFrom(metadata,removeProperty(envelope.payloadAsJsonObject(), "hearingId")));

    }
}