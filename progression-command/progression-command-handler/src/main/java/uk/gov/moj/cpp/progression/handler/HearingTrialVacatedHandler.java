package uk.gov.moj.cpp.progression.handler;

import static uk.gov.moj.cpp.progression.helper.EventStreamHelper.appendEventsToStream;

import uk.gov.justice.hearing.courts.ProgressionHearingTrialVacated;
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

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class HearingTrialVacatedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingTrialVacatedHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.hearing-trial-vacated")
    public void hearingTrialVacated(final Envelope<ProgressionHearingTrialVacated> hearingEnvelope) throws EventStreamException {

        LOGGER.debug("progression.command.hearing-trial-vacated {}", hearingEnvelope.payload());

        final ProgressionHearingTrialVacated hearingTrialVacated = hearingEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(hearingTrialVacated.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.hearingTrialVacated(hearingTrialVacated.getHearingId(), hearingTrialVacated.getVacatedTrialReasonId());
        appendEventsToStream(hearingEnvelope, eventStream, events);
    }

}