package uk.gov.moj.cpp.progression.command.handler;

import static java.util.UUID.fromString;

import java.time.LocalDate;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddConvictionDateHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.offence-conviction-date-changed")
    public void addConvictionDateToOffence(final JsonEnvelope command) throws EventStreamException {

        applyToCaseProgressionAggregate(command,
                aCase -> aCase.addConvictionDateToOffence(
                        fromString(command.payloadAsJsonObject().getString("caseId")),
                        fromString(command.payloadAsJsonObject().getString("offenceId")),
                        LocalDate.parse(command.payloadAsJsonObject().getString("convictionDate"))));
    }

}
