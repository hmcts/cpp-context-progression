package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.progression.helper.EventStreamHelper.appendEventsToStream;

import java.util.stream.Stream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.staginghmi.courts.UpdateHearingFromHmi;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

@ServiceComponent(Component.COMMAND_HANDLER)
public class HmiHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HmiHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.update-hearing-from-hmi")
    public void handleUpdateHearingHmi(final Envelope<UpdateHearingFromHmi> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-hearing-from-hmi {}", envelope.payload());
        }
        final UpdateHearingFromHmi updateHearingFromHmi = envelope.payload();
        if(isNull(updateHearingFromHmi.getCourtRoomId())) {
            final EventStream eventStream = eventSource.getStreamById(updateHearingFromHmi.getHearingId());
            final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

            final Stream<Object> events = hearingAggregate.updateHearing(updateHearingFromHmi);
            appendEventsToStream(envelope, eventStream, events);
        }
    }

}
