package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.BookSlotsForApplication;
import uk.gov.justice.core.courts.HearingListingNeeds;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00112", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class BookSlotsForApplicationHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BookSlotsForApplicationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.book-slots-for-application")
    public void handleBookSlotsForApplication(final Envelope<BookSlotsForApplication> bookSlotsForApplicationEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.book-slots-for-application {}", bookSlotsForApplicationEnvelope.payload());

        final BookSlotsForApplication bookSlotsForApplication = bookSlotsForApplicationEnvelope.payload();
        final HearingListingNeeds hearingRequest = bookSlotsForApplication.getHearingRequest();

        final EventStream eventStream = eventSource.getStreamById(hearingRequest.getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> bookSlotsEvent = applicationAggregate.bookSlotsForApplication(hearingRequest);
        appendEventsToStream(bookSlotsForApplicationEnvelope, eventStream, bookSlotsEvent);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
