package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.update-defendant")
    public void updateDefendant(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command, aCase -> aCase.updateDefendant(converter.convert(command.payloadAsJsonObject(), UpdateDefendantCommand.class)));
    }
}

