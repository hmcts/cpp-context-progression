package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.UnallocateHearingRemoveCourtroom;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class HearingUnallocatedCourtRoomRemovedHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUnallocatedCourtRoomRemovedHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.command.unallocate-hearing-remove-courtroom")
    public void handleHearingUnallocatedCourtRoomRemoved(final Envelope<UnallocateHearingRemoveCourtroom> envelope)
            throws EventStreamException {

        LOGGER.debug("progression.command.unallocate-hearing-remove-courtroom {}", envelope);

        final UnallocateHearingRemoveCourtroom unallocateHearingRemoveCourtroom = envelope.payload();
        final UUID hearingId = unallocateHearingRemoveCourtroom.getHearingId();
        final Integer estimatedMinutes = unallocateHearingRemoveCourtroom.getEstimatedMinutes();

        // aggregating based on unallocated hearing id
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.unallocateHearingWhenCourtroomIsRemoved(hearingId, estimatedMinutes);

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
