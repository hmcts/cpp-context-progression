package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ServiceComponent(Component.COMMAND_HANDLER)
public class CompleteSendingSheetHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.complete-sending-sheet")
    public void completeSendingSheet(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command,
                aCase -> aCase.completeSendingSheet(command));
    }
}
