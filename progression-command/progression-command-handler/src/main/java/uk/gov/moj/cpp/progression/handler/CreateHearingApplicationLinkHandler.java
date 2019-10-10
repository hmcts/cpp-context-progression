package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.CreateHearingApplicationLink;
import uk.gov.justice.core.courts.HearingListingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;


@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateHearingApplicationLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateHearingApplicationLinkHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.create-hearing-application-link")
    public void handle(final Envelope<CreateHearingApplicationLink> createHearingApplicationLinkEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.create-hearing-application-link {}", createHearingApplicationLinkEnvelope.payload());

        final CreateHearingApplicationLink createHearingApplicationLink = createHearingApplicationLinkEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(createHearingApplicationLink.getApplicationId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.createHearingApplicationLink(
                createHearingApplicationLink.getHearing(), createHearingApplicationLink.getApplicationId(), createHearingApplicationLink.getHearingListingStatus());
        appendEventsToStream(createHearingApplicationLinkEnvelope, eventStream, events);
        if (applicationAggregate.getBoxHearingId() != null) {
            final EventStream hearingEventStream = eventSource.getStreamById(applicationAggregate.getBoxHearingId());
            final HearingAggregate hearingAggregate = aggregateService.get(hearingEventStream, HearingAggregate.class);
            if (hearingAggregate.getSavedListingStatusChanged() != null && HearingListingStatus.HEARING_RESULTED == createHearingApplicationLink.getHearingListingStatus()) {
                appendEventsToStream(createHearingApplicationLinkEnvelope, hearingEventStream, hearingAggregate.boxworkComplete());
            }
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
