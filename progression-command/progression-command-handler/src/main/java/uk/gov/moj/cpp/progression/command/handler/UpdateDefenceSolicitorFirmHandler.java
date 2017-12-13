package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantDefenceSolicitorFirm;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefenceSolicitorFirmHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.update-defence-solicitor-firm-for-defendant")
    public void updateDefendantSolicitorFirmForDefendant(final JsonEnvelope command)
                    throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                        aCase -> aCase.updateDefenceSolicitorFirm(converter.convert(
                                        command.payloadAsJsonObject(), UpdateDefendantDefenceSolicitorFirm.class)));
    }

}
