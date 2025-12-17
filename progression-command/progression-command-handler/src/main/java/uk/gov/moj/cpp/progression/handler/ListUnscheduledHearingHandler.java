package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.ListUnscheduledHearing;
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
public class ListUnscheduledHearingHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.list-unscheduled-hearing")
    public void handleUnscheduledHearing(final Envelope<ListUnscheduledHearing> envelope) throws EventStreamException {
        final ListUnscheduledHearing listUnscheduledHearing = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(listUnscheduledHearing.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.listUnscheduledHearing(listUnscheduledHearing.getHearing());
        appendEventsToStream(envelope, eventStream, events);
    }
}
