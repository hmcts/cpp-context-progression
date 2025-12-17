package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.progression.courts.RemoveHearingForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class UnallocateHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnallocateHearingCommandHandler.class);

    private static final String PROGRESSION_COMMAND_REMOVE_HEARING_FOR_PROSECUTION_CASE = "progression.command.remove-hearing-for-prosecution-case";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles(PROGRESSION_COMMAND_REMOVE_HEARING_FOR_PROSECUTION_CASE)
    public void handleRemoveHearingForProsecutionCase(final Envelope<RemoveHearingForProsecutionCase> removeHearingForProsecutionCaseEnvelope) throws EventStreamException {

        log(PROGRESSION_COMMAND_REMOVE_HEARING_FOR_PROSECUTION_CASE, removeHearingForProsecutionCaseEnvelope.payload().getHearingId().toString());

        final RemoveHearingForProsecutionCase removeHearingForProsecutionCase = removeHearingForProsecutionCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(removeHearingForProsecutionCase.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = caseAggregate.removeHearingRelatedToProsecutionCase(removeHearingForProsecutionCase.getHearingId(), removeHearingForProsecutionCase.getProsecutionCaseId());

        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(removeHearingForProsecutionCaseEnvelope)));
    }

    private void log(final String eventName, final String hearingId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} event received with hearingId {}", eventName, hearingId);
        }
    }
}
