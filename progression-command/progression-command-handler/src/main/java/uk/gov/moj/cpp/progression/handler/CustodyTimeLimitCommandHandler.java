package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.ExtendCustodyTimeLimit;
import uk.gov.justice.progression.courts.StopCustodyTimeLimitClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CustodyTimeLimitCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustodyTimeLimitCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.stop-custody-time-limit-clock")
    public void handleStopCustodyTimeLimitClock(final Envelope<StopCustodyTimeLimitClock> stopCustodyTimeLimitClockEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'{}' received with payload {}", "progression.command.stop-custody-time-limit-clock", stopCustodyTimeLimitClockEnvelope);
        }

        final StopCustodyTimeLimitClock stopCustodyTimeLimitClock = stopCustodyTimeLimitClockEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(stopCustodyTimeLimitClock.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.stopCustodyTimeLimitClock(stopCustodyTimeLimitClock.getHearingId(), stopCustodyTimeLimitClock.getOffenceIds());
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(stopCustodyTimeLimitClockEnvelope)));

    }

    @Handles("progression.command.extend-custody-time-limit")
    public void handleExtendCustodyTimeLimit(final Envelope<ExtendCustodyTimeLimit> extendCustodyTimeLimitEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'{}' received with payload {}", "progression.command.extend-custody-time-limit", extendCustodyTimeLimitEnvelope);
        }

        final ExtendCustodyTimeLimit extendCustodyTimeLimit = extendCustodyTimeLimitEnvelope.payload();

        final EventStream eventStream = eventSource.getStreamById(extendCustodyTimeLimit.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.extendCustodyTimeLimit(extendCustodyTimeLimit.getHearingId(), extendCustodyTimeLimit.getOffenceId(), extendCustodyTimeLimit.getExtendedTimeLimit());
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(extendCustodyTimeLimitEnvelope)));
    }
}
