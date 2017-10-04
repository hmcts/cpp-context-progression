package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;

import javax.inject.Inject;
import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NoMoreInformationRequiredHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.record-no-more-information-required")
    public void noMoreInformationRequired(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.noMoreInformationForDefendant(
                        UUID.fromString(command.payloadAsJsonObject().getString("defendantId")), UUID.fromString(command.payloadAsJsonObject().getString("caseId")), UUID.fromString(command.payloadAsJsonObject().getString("caseProgressionId"))));
    }
}
