package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RequestPsrForDefendantHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.handler.request-psr-for-defendants")
    public void requestPsrForDefendants(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.requestPsrForDefendant(command));
    }



}
