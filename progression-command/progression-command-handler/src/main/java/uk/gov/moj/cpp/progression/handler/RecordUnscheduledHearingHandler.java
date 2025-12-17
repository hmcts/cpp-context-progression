package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.RecordUnscheduledHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import javax.inject.Inject;
import java.util.stream.Stream;

import static uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService.appendEventsToStream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RecordUnscheduledHearingHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.record-unscheduled-hearing")
    public void handleRecordUnscheduledHearing(final Envelope<RecordUnscheduledHearing> envelope) throws EventStreamException {
        final RecordUnscheduledHearing recordUnscheduledHearing = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(recordUnscheduledHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.recordUnscheduledHearing(recordUnscheduledHearing.getHearingId(), recordUnscheduledHearing.getUnscheduledHearingIds());
        appendEventsToStream(envelope, eventStream, events);
    }
}
