package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.UpdateDefendantForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-defendant-for-prosecution-case")
    public void handle(final Envelope<UpdateDefendantForProsecutionCase> updateDefendantEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-defendant-for-prosecution-case {}", updateDefendantEnvelope.payload());

        final Stream<Object> events;
        final UpdateDefendantForProsecutionCase defendantDetailsToUpdate = updateDefendantEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(defendantDetailsToUpdate.getDefendant().getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        if (defendantDetailsToUpdate.getMatchedDefendantHearingId() == null) {
            events = caseAggregate.updateDefendantDetails(defendantDetailsToUpdate.getDefendant());
        } else {
            events = caseAggregate.recordUpdateDefendantDetailsWithMatched(defendantDetailsToUpdate.getDefendant(), defendantDetailsToUpdate.getMatchedDefendantHearingId());
        }
        appendEventsToStream(updateDefendantEnvelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
