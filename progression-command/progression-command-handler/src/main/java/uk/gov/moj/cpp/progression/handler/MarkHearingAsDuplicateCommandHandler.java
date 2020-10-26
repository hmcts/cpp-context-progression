package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.progression.courts.MarkHearingAsDuplicate;
import uk.gov.justice.progression.courts.MarkHearingAsDuplicateForCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
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

@ServiceComponent(COMMAND_HANDLER)
public class MarkHearingAsDuplicateCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkHearingAsDuplicateCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.command.mark-hearing-as-duplicate")
    public void handleMarkHearingAsDuplicate(final Envelope<MarkHearingAsDuplicate> markHearingAsDuplicateEnvelope) throws EventStreamException {

        final MarkHearingAsDuplicate markHearingAsDuplicate = markHearingAsDuplicateEnvelope.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'progression.command.mark-hearing-as-duplicate' received with payload {}", markHearingAsDuplicate);
        }

        final EventStream eventStream = eventSource.getStreamById(markHearingAsDuplicate.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.markAsDuplicate(markHearingAsDuplicate.getHearingId(), markHearingAsDuplicate.getProsecutionCaseIds(), markHearingAsDuplicate.getDefendantIds());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(markHearingAsDuplicateEnvelope)));
    }

    @Handles("progression.command.mark-hearing-as-duplicate-for-case")
    public void handleMarkHearingAsDuplicateForCase(final Envelope<MarkHearingAsDuplicateForCase> markHearingAsDuplicateEnvelopeForCase) throws EventStreamException {

        final MarkHearingAsDuplicateForCase markHearingAsDuplicateForCase = markHearingAsDuplicateEnvelopeForCase.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'progression.command.mark-hearing-as-duplicate-for-case' received with payload {}", markHearingAsDuplicateEnvelopeForCase);
        }
        final EventStream eventStream = eventSource.getStreamById(markHearingAsDuplicateForCase.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.markHearingAsDuplicate(markHearingAsDuplicateForCase.getHearingId(), markHearingAsDuplicateForCase.getCaseId(), markHearingAsDuplicateForCase.getDefendantIds());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(markHearingAsDuplicateEnvelopeForCase)));
    }

}
