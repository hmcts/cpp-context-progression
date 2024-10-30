package uk.gov.moj.cpp.progression.handler;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.core.courts.UpdateRelatedHearingCommandForAdhocHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import javax.inject.Inject;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RelatedHearingCommandHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedHearingCommandHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-related-hearing")
    public void handleUpdateRelatedHearingCommand(final Envelope<UpdateRelatedHearingCommand> envelope) throws EventStreamException {
        LOGGER.info("progression.command.update-related-hearing event received with metadata {} and payload {}",
                envelope.metadata(), envelope.payload());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-related-hearing payload: {}", envelope.payload());
        }

        final UpdateRelatedHearingCommand command = envelope.payload();
        final HearingListingNeeds hearingRequest = command.getHearingRequest();
        final EventStream eventStream = eventSource.getStreamById(hearingRequest.getId());

        if (isNotEmpty(hearingRequest.getProsecutionCases())) {

            final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

            final Stream<Object> events = hearingAggregate.updateRelatedHearing(
                    hearingRequest,
                    command.getIsAdjourned(),
                    command.getExtendedHearingFrom(),
                    command.getIsPartiallyAllocated(),
                    command.getSeedingHearing(),
                    command.getShadowListedOffences());

            appendEventsToStream(envelope, eventStream, events);

        } else if (isNotEmpty(hearingRequest.getCourtApplications())) {
            // DD-8648: Related hearing change for Applications
        }
    }

    @Handles("progression.command.update-related-hearing-for-adhoc-hearing")
    public void handleUpdateRelatedHearingCommandForAdhocHearing(final Envelope<UpdateRelatedHearingCommandForAdhocHearing> envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-related-hearing-for-adhoc-hearing payload: {}", envelope.payload());
        }

        final UpdateRelatedHearingCommandForAdhocHearing command = envelope.payload();
        final HearingListingNeeds hearingRequest = command.getHearingRequest();
        final EventStream eventStream = eventSource.getStreamById(hearingRequest.getId());

        if (isNotEmpty(hearingRequest.getProsecutionCases())) {

            final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

            final Stream<Object> events = hearingAggregate.updateRelatedHearingForAdhocHearing(
                    hearingRequest,
                    command.getSendNotificationToParties());

            appendEventsToStream(envelope, eventStream, events);

        } else if (isNotEmpty(hearingRequest.getCourtApplications())) {
            // DD-8648: Related hearing change for Applications
        }
    }
}
