package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddDefendantHandler extends CaseProgressionCommandHandler{

    @Handles("progression.command.record-add-defendant")
    public void addDefendant(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command, aCase -> aCase.addDefendant(converter.convert(command.payloadAsJsonObject(), AddDefendant.class)));
    }
}
