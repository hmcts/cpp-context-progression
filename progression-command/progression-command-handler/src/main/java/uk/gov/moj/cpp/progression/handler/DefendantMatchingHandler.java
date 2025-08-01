package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.MatchDefendant;
import uk.gov.justice.core.courts.MatchedDefendants;
import uk.gov.justice.core.courts.ProcessMatchedDefendants;
import uk.gov.justice.core.courts.UpdateMatchedDefendant;
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
import uk.gov.moj.cpp.progression.domain.MatchedDefendant;
import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DefendantMatchingHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefendantMatchingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.process-matched-defendants")
    public void storeMatchedDefendants(final Envelope<ProcessMatchedDefendants> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.process-matched-defendants: {}", envelope.payload());
        }
        final ProcessMatchedDefendants processMatchedDefendants = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(processMatchedDefendants.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.storeMatchedDefendants(processMatchedDefendants.getProsecutionCaseId());
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.update-matched-defendant")
    public void updateMatchedDefendant(final Envelope<UpdateMatchedDefendant> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-matched-defendant: {}", envelope.payload());
        }
        final UpdateMatchedDefendant updateMatchedDefendant = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateMatchedDefendant.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateMatchedDefendant(updateMatchedDefendant.getProsecutionCaseId(), updateMatchedDefendant.getDefendantId(), updateMatchedDefendant.getMasterDefendantId(), updateMatchedDefendant.getProcessInactiveCase());
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.match-defendant")
    public void handle(final Envelope<MatchDefendant> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.match-defendant payload: {}", envelope.payload());
        }
        final MatchDefendant matchDefendant = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(matchDefendant.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.matchPartiallyMatchedDefendants(transformMatchDefendant(matchDefendant));
        appendEventsToStream(envelope, eventStream, events);
    }

    private uk.gov.moj.cpp.progression.domain.MatchDefendant transformMatchDefendant(final MatchDefendant matchDefendant) {
        return uk.gov.moj.cpp.progression.domain.MatchDefendant.matchDefendant()
                .withProsecutionCaseId(matchDefendant.getProsecutionCaseId())
                .withDefendantId(matchDefendant.getDefendantId())
                .withMatchedDefendants(transformMatchedDefendants(matchDefendant.getMatchedDefendants()))
                .build();
    }

    private List<MatchedDefendant> transformMatchedDefendants(final List<MatchedDefendants> matchedDefendants) {
        return matchedDefendants.stream()
                .map(def -> MatchedDefendant.matchedDefendant()
                        .withProsecutionCaseId(def.getProsecutionCaseId())
                        .withDefendantId(def.getDefendantId())
                        .withMasterDefendantId(def.getMasterDefendantId())
                        .withCourtProceedingsInitiated(def.getCourtProceedingsInitiated())
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
