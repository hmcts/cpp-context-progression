package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.progression.courts.DeleteApplicationForCase;
import uk.gov.justice.progression.courts.DeleteCourtApplicationHearing;
import uk.gov.justice.progression.courts.RemoveApplicationFromSeedingHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class DeleteApplicationCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteApplicationCommandHandler.class);
    private static final String RECEIVED_WITH_PAYLOAD = "'{}' received with payload {}";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.delete-application-for-case")
    public void handleDeleteApplicationForCase(Envelope<DeleteApplicationForCase> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_WITH_PAYLOAD, "progression.command.delete-application-for-case", envelope);
        }

        final DeleteApplicationForCase deleteCourtApplication = envelope.payload();
        final UUID applicationId = deleteCourtApplication.getApplicationId();
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.deleteCourtApplication(applicationId, deleteCourtApplication.getSeedingHearingId());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.delete-court-application-hearing")
    public void handleDeleteCourtApplicationHearing(Envelope<DeleteCourtApplicationHearing> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_WITH_PAYLOAD, "progression.command.delete-court-application-hearing", envelope);
        }

        final DeleteCourtApplicationHearing deleteCourtApplicationHearing = envelope.payload();
        final UUID hearingId = deleteCourtApplicationHearing.getHearingId();
        final UUID seedingHearingId = deleteCourtApplicationHearing.getSeedingHearingId();
        final UUID applicationId = deleteCourtApplicationHearing.getApplicationId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.deleteCourtApplicationHearing(hearingId,applicationId, seedingHearingId);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(envelope)));
    }


    @Handles("progression.command.remove-application-from-seedingHearing")
    public void handleRemoveApplicationFromSeedingHearing(Envelope<RemoveApplicationFromSeedingHearing> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_WITH_PAYLOAD, "progression.command.remove-application-from-seedingHearing", envelope);
        }

        final RemoveApplicationFromSeedingHearing removeApplicationFromSeedingHearing = envelope.payload();
        final UUID applicationId = removeApplicationFromSeedingHearing.getApplicationId();
        final UUID hearingId = removeApplicationFromSeedingHearing.getSeedingHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.removeApplicationFromSeedingHearing(hearingId, applicationId);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(envelope)));
    }
}
