package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;


import java.util.stream.Stream;
import javax.inject.Inject;
import javax.json.JsonValue;
import uk.gov.justice.core.courts.MoveOffencesFromOldNextHearing;
import uk.gov.justice.core.courts.MoveOffencesToNewNextHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

@ServiceComponent(COMMAND_HANDLER)
public class NextHearingCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.move-offences-from-old-next-hearing")
    public void handlerMoveOffencesFromOldNextHearing(final Envelope<MoveOffencesFromOldNextHearing> moveOffencesFromOldNextHearingEnvelope) throws EventStreamException {
        final MoveOffencesFromOldNextHearing moveOffencesFromOldNextHearing = moveOffencesFromOldNextHearingEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(moveOffencesFromOldNextHearing.getOldHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.moveOffencesFromHearing(moveOffencesFromOldNextHearing);

        appendEventsToStream(moveOffencesFromOldNextHearingEnvelope, eventStream, events);

    }

    @Handles("progression.command.move-offences-to-new-next-hearing")
    public void handlerMoveOffencesToNewNextHearing(final Envelope<MoveOffencesToNewNextHearing> moveOffencesToNewNextHearingEnvelope) throws EventStreamException {
        MoveOffencesToNewNextHearing moveOffencesToNewNextHearing = moveOffencesToNewNextHearingEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(moveOffencesToNewNextHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.moveOffencesToHearing(moveOffencesToNewNextHearing);

        appendEventsToStream(moveOffencesToNewNextHearingEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
