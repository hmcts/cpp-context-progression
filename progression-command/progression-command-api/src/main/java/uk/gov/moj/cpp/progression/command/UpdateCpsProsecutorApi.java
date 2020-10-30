package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;


@ServiceComponent(COMMAND_API)
public class UpdateCpsProsecutorApi {

    @Inject
    private Sender sender;

    @Handles("progression.update-cps-prosecutor-details")
    public void handleUpdateCpsProsecutorDetails(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.update-cps-prosecutor-details")
                .build();
        sender.send(envelopeFrom(metadata, envelope.payload()));
    }
}
