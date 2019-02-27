package uk.gov.moj.cpp.progression.command.handler;

import javax.inject.Inject;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.convertor.OffencesForDefendantConverter;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateOffencesForDefendantHandler extends CaseProgressionCommandHandler {
    @Inject
    private OffencesForDefendantConverter offencesForDefendantConverter;

    @Handles("progression.command.update-offences-for-defendant")
    public void updateOffences(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aCase -> aCase.updateOffencesForDefendant(offencesForDefendantConverter.convert(command)));
    }
}

