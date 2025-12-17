package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AssignDefendantRequestToExtendHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import javax.inject.Inject;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AssignDefendantRequestToExtendHearingHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignDefendantRequestToExtendHearingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.assign-defendant-request-to-extend-hearing")
    public void assignDefendantRequestToExtendHearing(final Envelope<AssignDefendantRequestToExtendHearing> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.assign-defendant-request-to-extend-hearing payload {}", envelope.payload());
        }

        final AssignDefendantRequestToExtendHearing command = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.assignDefendantRequestToExtendHearing(command.getHearingId(), command.getDefendantRequests());

        appendEventsToStream(envelope, eventStream, events);
    }
}
