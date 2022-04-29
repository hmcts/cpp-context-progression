package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.PopulateHearingToProbationCaseworker;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PopulateHearingToProbationCaseworkerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateHearingToProbationCaseworkerHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.populate-hearing-to-probation-caseworker")
    public void handlePopulateHearingToProbationCaseworker(final Envelope<PopulateHearingToProbationCaseworker> populateHearingToProbationCaseworkerEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'{}' received with payload {}", "progression.command.populate-hearing-to-probation-caseworker", populateHearingToProbationCaseworkerEnvelope);
        }

        final PopulateHearingToProbationCaseworker populateHearingToProbationCaseworker = populateHearingToProbationCaseworkerEnvelope.payload();
        final UUID hearingId = populateHearingToProbationCaseworker.getHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(populateHearingToProbationCaseworkerEnvelope)));
    }
}
