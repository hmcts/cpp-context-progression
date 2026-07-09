package uk.gov.moj.cpp.progression.handler;

import static uk.gov.moj.cpp.progression.helper.EventStreamHelper.appendEventsToStream;

import uk.gov.justice.progression.courts.ReplayHearingConfirmed;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ReplayHearingConfirmedHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplayHearingConfirmedHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.replay-hearing-confirmed")
    public void replayHearingConfirmed(final Envelope<ReplayHearingConfirmed> replayHearingConfirmedEnvelope) throws EventStreamException {

        ReplayHearingConfirmed replayHearingConfirmed = replayHearingConfirmedEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(replayHearingConfirmed.getConfirmedHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.replayHearingConfirmed(replayHearingConfirmed);

        appendEventsToStream(replayHearingConfirmedEnvelope, eventStream, events);
    }
}
