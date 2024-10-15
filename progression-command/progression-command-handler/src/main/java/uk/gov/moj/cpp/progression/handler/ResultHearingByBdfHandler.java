package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.command.ResultHearingBdf;
import uk.gov.moj.cpp.progression.command.UpdateHearingBdf;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class ResultHearingByBdfHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultHearingByBdfHandler.class);

   private static final String RECEIVED_WITH_PAYLOAD = "'{}' received with payload {}";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.handler.result-hearing-bdf")
    public void handleDeleteHearing(final Envelope<ResultHearingBdf> hearingResultByBdfEnvelope) throws EventStreamException {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(RECEIVED_WITH_PAYLOAD, "progression.command.handler.result-hearing-bdf" , hearingResultByBdfEnvelope);
        }
        final ResultHearingBdf resultHearingBdf = hearingResultByBdfEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(resultHearingBdf.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.resultHearingByBdf(resultHearingBdf.getHearingId());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(hearingResultByBdfEnvelope)));
    }

    @Handles("progression.command.handler.update-hearing-bdf")
    public void handleUpdateHearing(final Envelope<UpdateHearingBdf> updateHearingBdfEnvelope) throws EventStreamException {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(RECEIVED_WITH_PAYLOAD, "progression.command.handler.update-hearing-bdf" , updateHearingBdfEnvelope);
        }
        final UpdateHearingBdf updateHaringBdf = updateHearingBdfEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateHaringBdf.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateHearingByBdf(updateHaringBdf.getHearingId(), updateHaringBdf.getProsecutionCaseId(), updateHaringBdf.getDefendantId(), updateHaringBdf.getOffenceId(), updateHaringBdf.getDefendantCaseJudicialResults(), updateHaringBdf.getOffenceJudicialResults());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(updateHearingBdfEnvelope)));
    }
}
