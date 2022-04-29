package uk.gov.moj.cpp.progression.handler;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.progression.courts.AddCasesForUpdatedRelatedHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddCasesForUpdatedRelatedHearingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddCasesForUpdatedRelatedHearingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.add-cases-for-updated-related-hearing")
    public void handleAddCasesForUpdateRelatedHearing(final Envelope<AddCasesForUpdatedRelatedHearing> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-cases-for-updated-related-hearing {}", envelope);
        }

        final AddCasesForUpdatedRelatedHearing addCasesForUpdatedRelatedHearing = envelope.payload();
        final UUID hearingId = addCasesForUpdatedRelatedHearing.getHearingId();
        final UUID seedingHearingId = addCasesForUpdatedRelatedHearing.getSeedingHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.addCasesForUpdatedRelatedHearing(seedingHearingId);

        appendEventsToStream(envelope, eventStream, events);
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        final Stream<JsonEnvelope> envelopeStream = events.map(toEnvelopeWithMetadataFrom(jsonEnvelope));
        eventStream.append(envelopeStream);
    }

}

