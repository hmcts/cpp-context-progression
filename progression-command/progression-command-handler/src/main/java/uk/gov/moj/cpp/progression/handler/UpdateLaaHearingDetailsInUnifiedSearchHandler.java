package uk.gov.moj.cpp.progression.handler;

import static uk.gov.moj.cpp.progression.helper.EventStreamHelper.appendEventsToStream;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.command.handler.courts.UpdateHearingDetailsInUnifiedSearch;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateLaaHearingDetailsInUnifiedSearchHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateLaaHearingDetailsInUnifiedSearchHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.handler.update-hearing-details-in-unified-search")
    public void handle(final Envelope<UpdateHearingDetailsInUnifiedSearch> updateLaaHearingDetailsInUnifiedSearchEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.update-hearing-details-in-unified-search {}", updateLaaHearingDetailsInUnifiedSearchEnvelope);


        final UpdateHearingDetailsInUnifiedSearch payload = updateLaaHearingDetailsInUnifiedSearchEnvelope.payload();

        final UUID hearingId = payload.getHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.updateHearingDetailsInUnifiedSearch(hearingId);
        appendEventsToStream(updateLaaHearingDetailsInUnifiedSearchEnvelope, eventStream, events);
    }

}

