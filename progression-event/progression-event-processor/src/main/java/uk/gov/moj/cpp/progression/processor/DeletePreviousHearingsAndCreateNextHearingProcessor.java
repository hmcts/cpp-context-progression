package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@ServiceComponent(EVENT_PROCESSOR)
public class DeletePreviousHearingsAndCreateNextHearingProcessor {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.event.delete-previous-hearings-and-create-next-hearing")
    public void processDeletePreviousHearingsAndCreateNextHearingRequestedEvent(final JsonEnvelope event) {
        sender.send(
                envelop(event.payload())
                        .withName("listing.delete-previous-hearings-and-create-next-hearing")
                        .withMetadataFrom(event));
    }
}
