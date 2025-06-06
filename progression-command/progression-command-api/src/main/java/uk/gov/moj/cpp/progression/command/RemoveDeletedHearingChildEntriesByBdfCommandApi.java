package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

/**
 * The command API is being added only to be called from BDF, to delete the child entries of already deleted hearing entries from the progression view store.
 */
@ServiceComponent(COMMAND_API)
public class RemoveDeletedHearingChildEntriesByBdfCommandApi {

    @Inject
    private Sender sender;

    @Handles("progression.command.remove-deleted-hearing-child-entries-bdf")
    public void handle(final JsonEnvelope envelope) {
        /**
         * DO NOT USE THIS COMMAND API EXCEPT FOR THE PURPOSE MENTIONED BELOW.
         * The command api is being added to be invoked only by the BDF, purpose of this command to raise 'progression.event.hearing-deleted'
         * event to remove any child entries of deleted hearing entity from the view store.
         */
        sender.send(Enveloper
                .envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.handler.remove-deleted-hearing-child-entries-bdf")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.add-case-to-hearing-bdf")
    public void handleAddCaseToHearing(final JsonEnvelope envelope) {
        /**
         * DO NOT USE THIS COMMAND API EXCEPT FOR THE PURPOSE MENTIONED BELOW.
         * The command api is being added to be invoked only by the BDF, purpose of this command to raise 'progression.event.case-added-to-hearing-bdf'
         * event to add case to hearing aggregate and viewstore to fix faulty hearings.
         */
        sender.send(Enveloper
                .envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.add-case-to-hearing-bdf")
                .withMetadataFrom(envelope));
    }


}
