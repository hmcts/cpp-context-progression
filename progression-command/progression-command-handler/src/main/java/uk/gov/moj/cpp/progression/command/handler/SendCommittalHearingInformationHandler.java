package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.stream.Stream;

import static uk.gov.justice.services.eventsourcing.source.core.Events.streamOf;

@ServiceComponent(Component.COMMAND_HANDLER)
public class SendCommittalHearingInformationHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.handler.sending-committal-hearing-information")
    public void sendCommittalHearingInformation(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.sendingHearingCommittal(command));
    }
}
