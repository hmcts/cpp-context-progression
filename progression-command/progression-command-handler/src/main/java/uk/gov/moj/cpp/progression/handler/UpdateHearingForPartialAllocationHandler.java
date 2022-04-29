package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
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

@SuppressWarnings({"squid:S00112", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateHearingForPartialAllocationHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UpdateHearingForPartialAllocationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-hearing-for-partial-allocation")
    public void handle(final Envelope<UpdateHearingForPartialAllocation> updateHearingForPartialAllocationEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-hearing-for-partial-allocation {}", updateHearingForPartialAllocationEnvelope.payload());

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = updateHearingForPartialAllocationEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateHearingForPartialAllocation.getHearingId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateHearingForPartialAllocation(updateHearingForPartialAllocation.getHearingId(), updateHearingForPartialAllocation.getProsecutionCasesToRemove());
        appendEventsToStream(updateHearingForPartialAllocationEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
