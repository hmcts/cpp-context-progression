package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.UpdateHearingOffencePlea;
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

@SuppressWarnings("squid:CallToDeprecatedMethod")
@ServiceComponent(Component.COMMAND_HANDLER)
public class PleaUpdatedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaUpdatedHandler.class);
    private static final String UPDATE_HEARING_OFFENCE_PLEA = "progression.command.update-hearing-offence-plea";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles(UPDATE_HEARING_OFFENCE_PLEA)
    public void handleUpdatePlea(final Envelope<UpdateHearingOffencePlea> envelope) throws EventStreamException {
        LOGGER.debug(UPDATE_HEARING_OFFENCE_PLEA + " {}", envelope);
        final UpdateHearingOffencePlea updateHearingOffencePlea = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateHearingOffencePlea.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateHearingWithPlea(updateHearingOffencePlea.getPleaModel());
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
