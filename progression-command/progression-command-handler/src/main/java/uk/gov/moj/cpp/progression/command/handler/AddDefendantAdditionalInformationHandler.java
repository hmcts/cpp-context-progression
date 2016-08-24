package uk.gov.moj.cpp.progression.command.handler;

import javax.inject.Inject;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddDefendantAdditionalInformationHandler extends CaseProgressionCommandHandler {

    @Inject
    ProgressionEventFactory progressionEventFactory;

    @Handles("progression.command.add-defendant-additional-information")
    public void addAdditionalInformationForDefendant(final JsonEnvelope command)
                    throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                        aCase -> aCase.addAdditionalInformationForDefendant(
                                        progressionEventFactory.addDefendantEvent(converter.convert(
                                                        command.payloadAsJsonObject(), DefendantCommand.class))));
    }
}
