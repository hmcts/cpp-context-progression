package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddSentenceHearingHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.record-sentence-hearing")
    public void addSentenceHearing(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.addSentenceHearing(
                         UUID.fromString(command.payloadAsJsonObject().getString("caseId")), UUID.fromString(command.payloadAsJsonObject().getString("caseProgressionId")), UUID.fromString(command.payloadAsJsonObject().getString("sentenceHearingId"))));
    }


}
