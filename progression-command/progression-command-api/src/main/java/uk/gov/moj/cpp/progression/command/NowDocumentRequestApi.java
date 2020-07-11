package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class NowDocumentRequestApi {

    @Inject
    private Sender sender;

    @Handles("progression.add-now-document-request")
    public void addNowDocumentRequest(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.add-now-document-request").build(),
                command.payloadAsJsonObject()));
    }
}
