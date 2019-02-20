package uk.gov.moj.cpp.progression.command.handler;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(Component.COMMAND_HANDLER)
public class SentenceHearingDateHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.record-sentence-hearing-date")
    public void addSentenceHearingDate(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command,
                aCase -> aCase.addSentenceHearingDate(
                         UUID.fromString(command.payloadAsJsonObject().getString("caseId")),
                        LocalDate.parse(command.payloadAsJsonObject().getString("sentenceHearingDate"))));
    }


}
