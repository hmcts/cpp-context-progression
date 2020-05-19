package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.UnmatchDefendant;
import uk.gov.justice.core.courts.UnmatchDefendants;
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
import uk.gov.moj.cpp.progression.domain.UnmatchedDefendant;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DefendantUnmatchingHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefendantUnmatchingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.unmatch-defendant")
    public void handle(final Envelope<UnmatchDefendant> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.unmatch-defendant payload: {}", envelope.payload());
        }
        final UnmatchDefendant unmatchDefendant = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(unmatchDefendant.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.unmatchDefendants(transformUnmatchDefendant(unmatchDefendant));
        appendEventsToStream(envelope, eventStream, events);
    }

    private uk.gov.moj.cpp.progression.domain.UnmatchDefendant transformUnmatchDefendant(final UnmatchDefendant unmatchDefendant) {
        return uk.gov.moj.cpp.progression.domain.UnmatchDefendant.unmatchDefendant()
                .withUnmatchedDefendants(transformUnmatchedDefendants(unmatchDefendant.getUnmatchDefendants()))
                .build();
    }

    private List<UnmatchedDefendant> transformUnmatchedDefendants(final List<UnmatchDefendants> unmatchedDefendants) {
        return unmatchedDefendants.stream()
                .map(def -> UnmatchedDefendant.unmatchedDefendant()
                        .withProsecutionCaseId(def.getProsecutionCaseId())
                        .withDefendantId(def.getDefendantId())
                        .build())
                .collect(Collectors.toList());
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
