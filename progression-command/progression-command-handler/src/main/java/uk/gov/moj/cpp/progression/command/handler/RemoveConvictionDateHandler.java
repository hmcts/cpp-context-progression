package uk.gov.moj.cpp.progression.command.handler;

import java.util.UUID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RemoveConvictionDateHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.offence-conviction-date-removed")
    public void removeConvictionDateFromOffence(final JsonEnvelope command) throws EventStreamException {

        applyToCaseProgressionAggregate(command,
                aCase -> aCase.removeConvictionDateFromOffence(
                        UUID.fromString(command.payloadAsJsonObject().getString("caseId")),
                        UUID.fromString(command.payloadAsJsonObject().getString("offenceId"))));
    }

}
