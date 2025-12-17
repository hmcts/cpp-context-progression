package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

/**
 * The command API is being added only to be called from BDF, to result hearing entries from the progression view store.
 */
@ServiceComponent(COMMAND_API)
public class ResultHearingByBdfCommandApi {

    @Inject
    private Sender sender;

    @Handles("progression.result-hearing-bdf")
    public void handle(final JsonEnvelope envelope) {
        /**
         * DO NOT USE THIS COMMAND API EXCEPT FOR THE PURPOSE MENTIONED BELOW.
         * The command api is being added to be invoked only by the BDF, purpose of this command to raise 'progression.event.hearing-resulted-bdf'
         * event to result a hearing in view store.
         */
        sender.send(Enveloper
                .envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.handler.result-hearing-bdf")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.command.api-update-hearing-bdf")
    public void handleUpdateHearingBdf(final JsonEnvelope envelope) {
        /**
         * DO NOT USE THIS COMMAND API EXCEPT FOR THE PURPOSE MENTIONED BELOW.
         * The command api is being added to be invoked only by the BDF, purpose of this command to raise 'progression.event.hearing-resulted-bdf'
         * event to add missing results from a hearing in view store.
         */
        sender.send(Enveloper
                .envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.handler.update-hearing-bdf")
                .withMetadataFrom(envelope));
    }
}
