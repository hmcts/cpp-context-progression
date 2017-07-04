package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PrepareForSentenceHearingHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.prepare-for-sentence-hearing")
    public void prepareForSentenceHearing(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.prepareForSentenceHearing(command));
    }


}
