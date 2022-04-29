package uk.gov.moj.cpp.progression.handler;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.HearingResultedUpdateCase;
import uk.gov.justice.core.courts.UpdateListingNumberToProsecutionCase;
import uk.gov.justice.progression.courts.IncreaseListingNumberToProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.Collections;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCaseHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.hearing-resulted-update-case")
    public void handle(final Envelope<HearingResultedUpdateCase> hearingResultedUpdateCaseEnvelope) throws EventStreamException {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.hearing-resulted-update-case {}", hearingResultedUpdateCaseEnvelope.payload());
        }
        final HearingResultedUpdateCase hearingUpdate = hearingResultedUpdateCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(hearingUpdate.getProsecutionCase().getId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateCase(hearingUpdate.getProsecutionCase(), isNotEmpty(hearingUpdate.getDefendantJudicialResults()) ? hearingUpdate.getDefendantJudicialResults() : Collections.emptyList());

        appendEventsToStream(hearingResultedUpdateCaseEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-listing-number-to-prosecution-case")
    public void handleUpdateListingNumber(final Envelope<UpdateListingNumberToProsecutionCase> updateListingNumberToProsecutionCaseEnvelope) throws EventStreamException {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-listing-number-to-prosecution-case {}", updateListingNumberToProsecutionCaseEnvelope.payload());
        }
        final UpdateListingNumberToProsecutionCase updateListingNumberToProsecutionCase = updateListingNumberToProsecutionCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateListingNumberToProsecutionCase.getProsecutionCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateListingNumber(updateListingNumberToProsecutionCase.getOffenceListingNumbers());

        appendEventsToStream(updateListingNumberToProsecutionCaseEnvelope, eventStream, events);
    }

    @Handles("progression.command.increase-listing-number-to-prosecution-case")
    public void handleIncreaseListingNumber(final Envelope<IncreaseListingNumberToProsecutionCase> increaseListingNumberForProsecutionCaseEnvelope) throws EventStreamException {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.increase-listing-number-to-prosecution-case {}", increaseListingNumberForProsecutionCaseEnvelope.payload());
        }
        final IncreaseListingNumberToProsecutionCase increaseListingNumberForProsecutionCase = increaseListingNumberForProsecutionCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(increaseListingNumberForProsecutionCase.getProsecutionCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.increaseListingNumber(increaseListingNumberForProsecutionCase.getOffenceIds(), increaseListingNumberForProsecutionCase.getHearingId());

        appendEventsToStream(increaseListingNumberForProsecutionCaseEnvelope, eventStream, events);
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
