package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.UpdateDefendantHearingResult;
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

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefedantHearingResultsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefedantHearingResultsHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-defendant-hearing-result")
    public void handle(final Envelope<UpdateDefendantHearingResult> updateDefendantHearingResultEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-defendant-hearing-results : {}", updateDefendantHearingResultEnvelope.payload());

        final UpdateDefendantHearingResult updateDefendantHearingResult = updateDefendantHearingResultEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantHearingResult.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateDefendantHearingResult(updateDefendantHearingResult.getHearingId(), updateDefendantHearingResult.getSharedResultLines());
        appendEventsToStream(updateDefendantHearingResultEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
