package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantInterpreter;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantInterpreterHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.update-interpreter-for-defendant")
    public void updateInterpreterForDefendant(final JsonEnvelope command)
            throws EventStreamException {
        applyToCaseProgressionAggregate(command, aCase -> aCase.updateDefendantInterpreter(converter
                .convert(command.payloadAsJsonObject(), UpdateDefendantInterpreter.class)));
    }

}
