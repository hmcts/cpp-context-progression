package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.SplitHearingCommand;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs a split by reusing the two flows progression already has: it lists the moved offences as
 * a brand new hearing, then removes them from the source hearing. No new event types — the existing
 * {@code list-hearing-requested} and {@code hearing-updated-for-partial-allocation} carry it, so
 * every downstream processor, listener and public publication is unchanged.
 *
 * <p><strong>Order matters.</strong> The new hearing is raised first and the removal second. A crash
 * between the two then leaves the offences listed twice — visible and recoverable — rather than
 * unlisted, which would silently drop them from every court list.
 */
@ServiceComponent(COMMAND_HANDLER)
public class SplitHearingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitHearingHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.split-hearing")
    public void handle(final Envelope<SplitHearingCommand> splitHearingEnvelope) throws EventStreamException {
        final SplitHearingCommand splitHearing = splitHearingEnvelope.payload();
        final UUID sourceHearingId = splitHearing.getHearingId();
        final UUID newHearingId = randomUUID();

        LOGGER.info("split-hearing: raising hearing {} from source hearing {}", newHearingId, sourceHearingId);

        final EventStream newHearingStream = eventSource.getStreamById(newHearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(newHearingStream, HearingAggregate.class);
        appendEventsToStream(splitHearingEnvelope, newHearingStream,
                hearingAggregate.listNewHearing(newHearingId,
                        splitHearing.getListNewHearing(),
                        splitHearing.getSendNotificationToParties()));

        final EventStream sourceHearingStream = eventSource.getStreamById(sourceHearingId);
        final CaseAggregate caseAggregate = aggregateService.get(sourceHearingStream, CaseAggregate.class);
        appendEventsToStream(splitHearingEnvelope, sourceHearingStream,
                caseAggregate.updateHearingForPartialAllocation(sourceHearingId,
                        splitHearing.getProsecutionCasesToRemove()));
    }

    private void appendEventsToStream(final Envelope<?> envelope,
                                      final EventStream eventStream,
                                      final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
