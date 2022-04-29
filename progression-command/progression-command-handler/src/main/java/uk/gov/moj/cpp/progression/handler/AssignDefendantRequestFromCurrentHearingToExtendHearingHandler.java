package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AssignDefendantRequestFromCurrentHearingToExtendHearing;
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
public class AssignDefendantRequestFromCurrentHearingToExtendHearingHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignDefendantRequestFromCurrentHearingToExtendHearingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing")
    public void assignDefendantRequestFromCurrentHearingToExtendHearing(final Envelope<AssignDefendantRequestFromCurrentHearingToExtendHearing> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing payload {}", envelope.payload());
        }

        final AssignDefendantRequestFromCurrentHearingToExtendHearing command = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(command.getCurrentHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.assignDefendantRequestFromCurrentHearingToExtendHearing(command.getCurrentHearingId(), command.getExtendHearingId());

        appendEventsToStream(envelope, eventStream, events);
    }
}
