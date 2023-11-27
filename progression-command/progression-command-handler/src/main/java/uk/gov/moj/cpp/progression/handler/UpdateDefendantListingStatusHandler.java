package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.UpdateDefendantListingStatusV2;
import uk.gov.justice.core.courts.UpdateDefendantListingStatusV3;
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
public class UpdateDefendantListingStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantListingStatusHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.update-defendant-listing-status")
    public void handle(final Envelope<UpdateDefendantListingStatusV2> updateDefendantListingStatusEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-defendant-listing-status {}", updateDefendantListingStatusEnvelope.payload());

        final UpdateDefendantListingStatusV2 updateDefendantListingStatus = updateDefendantListingStatusEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantListingStatus.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateDefendantListingStatus(updateDefendantListingStatus.getHearing(), updateDefendantListingStatus.getHearingListingStatus(),
                updateDefendantListingStatus.getNotifyNCES(), updateDefendantListingStatus.getListHearingRequests());
        appendEventsToStream(updateDefendantListingStatusEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-defendant-listing-status-v3")
    public void handleV3(final Envelope<UpdateDefendantListingStatusV3> updateDefendantListingStatusEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-defendant-listing-status-v3 {}", updateDefendantListingStatusEnvelope.payload());

        final UpdateDefendantListingStatusV3 updateDefendantListingStatus = updateDefendantListingStatusEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantListingStatus.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateDefendantListingStatusV3                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   (updateDefendantListingStatus.getHearing(), updateDefendantListingStatus.getHearingListingStatus(),
                updateDefendantListingStatus.getNotifyNCES(), updateDefendantListingStatus.getListNextHearings());
        appendEventsToStream(updateDefendantListingStatusEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
