package uk.gov.moj.cpp.progression.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CreateHearingForApplicationV2;
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

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateHearingForApplicationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateHearingForApplicationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.create-hearing-for-application")
    public void handle(final Envelope<CreateHearingForApplicationV2> createHearingForApplicationEnvelope) throws EventStreamException {

        LOGGER.debug("progression.command.create-hearing-for-application {}", createHearingForApplicationEnvelope.payload());

        final CreateHearingForApplicationV2 createHearingForApplication = createHearingForApplicationEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(createHearingForApplication.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.createHearingForApplication(createHearingForApplication.getHearing(), createHearingForApplication.getHearingListingStatus(), createHearingForApplication.getListHearingRequests());
        appendEventsToStream(createHearingForApplicationEnvelope, eventStream, events);


    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
