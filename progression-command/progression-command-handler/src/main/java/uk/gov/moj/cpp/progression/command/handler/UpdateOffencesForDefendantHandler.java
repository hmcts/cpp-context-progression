package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.convertor.OffencesForDefendantConverter;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateOffencesForDefendantHandler extends CaseProgressionCommandHandler {
    @Inject
    private OffencesForDefendantConverter offencesForDefendantConverter;

    @Handles("progression.command.handler.update-offences-for-defendant")
    public void updateOffences(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command, aCase -> aCase.updateOffencesForDefendant(offencesForDefendantConverter.convert(command)));
    }
}

