package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class SentenceHearingDateHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.record-sentence-hearing-date")
    public void addSentenceHearingDate(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.addSentenceHearingDate(
                         UUID.fromString(command.payloadAsJsonObject().getString("caseId")),
                        LocalDate.parse(command.payloadAsJsonObject().getString("sentenceHearingDate"))));
    }


}
