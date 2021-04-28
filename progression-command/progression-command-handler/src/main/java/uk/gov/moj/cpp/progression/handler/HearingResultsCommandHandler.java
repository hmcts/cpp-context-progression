package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class HearingResultsCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultsCommandHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.process-hearing-results")
    public void processHearingResults(final Envelope<HearingResult> envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.process-hearing-results {}", envelope);
        }

        final HearingResult hearingResult = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(hearingResult.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.processHearingResults(hearingResult.getHearing(), hearingResult.getSharedTime(), hearingResult.getShadowListedOffences(), hearingResult.getHearingDay());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.store-booking-reference-court-schedule-ids")
    public void processStoreBookingReferencesWithCourtScheduleIdsCommand(final Envelope<StoreBookingReferenceCourtScheduleIds> envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.store-booking-reference-court-schedule-ids {}", envelope);
        }

        final StoreBookingReferenceCourtScheduleIds command = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.storeBookingReferencesWithCourtScheduleIds(command.getBookingReferenceCourtScheduleIds(), command.getHearingDay());

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
