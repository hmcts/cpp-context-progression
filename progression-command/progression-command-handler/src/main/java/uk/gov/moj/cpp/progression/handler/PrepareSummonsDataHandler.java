package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.PrepareSummonsData;
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

@SuppressWarnings("squid:S3655")
@ServiceComponent(Component.COMMAND_HANDLER)
public class PrepareSummonsDataHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareSummonsDataHandler.class.getName());

    @Handles("progression.command.prepare-summons-data")
    public void prepareSummonsData(final Envelope<PrepareSummonsData> prepareSummonsDataEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.prepare-summons-data {}", prepareSummonsDataEnvelope);
        final PrepareSummonsData requestSummons = prepareSummonsDataEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(requestSummons.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.createSummonsData(requestSummons.getCourtCentre(), requestSummons.getHearingDateTime(), requestSummons.getConfirmedProsecutionCaseIds());
        if(events!=null) {
            appendEventsToStream(prepareSummonsDataEnvelope, eventStream, events);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }


}
