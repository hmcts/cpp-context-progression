package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultEventProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("public.hearing.resulted")
    public void handleHearingResultedPublicEvent(final JsonEnvelope event) {
        this.sender.send(this.enveloper.withMetadataFrom(event, "progression.command.hearing-result")
                .apply(event.payloadAsJsonObject()));
    }

}
