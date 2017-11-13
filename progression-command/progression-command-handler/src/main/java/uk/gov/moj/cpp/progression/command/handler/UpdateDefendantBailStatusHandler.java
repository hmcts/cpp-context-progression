package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantBailStatusHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.update-bail-status-for-defendant")
    public void updateBailStatusForDefendant(final JsonEnvelope command)
            throws EventStreamException {
        JsonObject payload = command.payloadAsJsonObject();
        UUID defendantId = UUID.fromString(payload.getString("defendantId"));

        final Optional<BailDocument> bailDocument = bailDocumentFrom(payload);
        final Optional<LocalDate> custodyTimeLimitDate =  custodyTimeLimitDateFrom(payload);

        applyToCaseProgressionAggregate(command, aCase -> aCase.updateDefendantBailStatus(
                defendantId,
                payload.getString("bailStatus"),
                bailDocument,
                custodyTimeLimitDate));
    }

    private Optional<BailDocument> bailDocumentFrom(JsonObject payload) {
        if(!payload.containsKey("documentId")) {
            return empty();
        }
        return of(new BailDocument(
                UUID.randomUUID(),
                UUID.fromString(payload.getString("documentId"))
        ));
    }

    private Optional<LocalDate> custodyTimeLimitDateFrom(JsonObject payload) {
        if(!payload.containsKey("custodyTimeLimitDate")) {
            return empty();
        }
        return of(LocalDates.from(payload.getString("custodyTimeLimitDate")));
    }

}
