package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.HearingResultedUpdateCase;
import uk.gov.justice.core.courts.ProsecutionCase;
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
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.List;

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

        final HearingResultedUpdateCase hearingUpdate = hearingResultedUpdateCaseEnvelope.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.hearing-resulted-update-case  hearingId{}", hearingUpdate.getHearingId());
        }
        final EventStream eventStream = eventSource.getStreamById(hearingUpdate.getProsecutionCase().getId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final List<DefendantJudicialResult> defendantJudicialResults = isNotEmpty(hearingUpdate.getDefendantJudicialResults()) ? hearingUpdate.getDefendantJudicialResults() : emptyList();

        final Stream<Object> events = caseAggregate.updateCase(hearingUpdate.getProsecutionCase(),
                defendantJudicialResults, hearingUpdate.getCourtCentre(),
                hearingUpdate.getHearingId(), hearingUpdate.getHearingType(),
                hearingUpdate.getJurisdictionType(), hearingUpdate.getIsBoxHearing(), hearingUpdate.getRemitResultIds());

        appendEventsToStream(hearingResultedUpdateCaseEnvelope, eventStream, events);

        if (nonNull(hearingUpdate.getProsecutionCase().getIsGroupMaster()) && hearingUpdate.getProsecutionCase().getIsGroupMaster()) {
            final EventStream groupCaseStream = eventSource.getStreamById(hearingUpdate.getProsecutionCase().getGroupId());
            final GroupCaseAggregate groupCaseAggregate = aggregateService.get(groupCaseStream, GroupCaseAggregate.class);
            for (final UUID memberCaseId : groupCaseAggregate.getMemberCases()) {
                if (hearingUpdate.getProsecutionCase().getId().compareTo(memberCaseId) != 0) {
                    final EventStream caseEventStream = eventSource.getStreamById(memberCaseId);
                    final CaseAggregate memberCaseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);
                    final ProsecutionCase memberCase = ProsecutionCase.prosecutionCase()
                            .withValuesFrom(memberCaseAggregate.getProsecutionCase())
                            .withCaseStatus(hearingUpdate.getProsecutionCase().getCaseStatus())
                            .build();
                    final Stream<Object> memberCaseEvent = memberCaseAggregate.updateCase(memberCase,
                            isNotEmpty(hearingUpdate.getDefendantJudicialResults()) ? hearingUpdate.getDefendantJudicialResults() : Collections.emptyList(),
                            hearingUpdate.getCourtCentre(), hearingUpdate.getHearingId(), hearingUpdate.getHearingType(),
                            hearingUpdate.getJurisdictionType(), hearingUpdate.getIsBoxHearing(), hearingUpdate.getRemitResultIds()
                    );

                    appendEventsToStream(hearingResultedUpdateCaseEnvelope, caseEventStream, memberCaseEvent);
                }
            }
        }
    }

    @Handles("progression.command.update-listing-number-to-prosecution-case")
    public void handleUpdateListingNumber(final Envelope<UpdateListingNumberToProsecutionCase> updateListingNumberToProsecutionCaseEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-listing-number-to-prosecution-case  caseId {}", updateListingNumberToProsecutionCaseEnvelope.payload().getProsecutionCaseId());
        }
        final UpdateListingNumberToProsecutionCase updateListingNumberToProsecutionCase = updateListingNumberToProsecutionCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateListingNumberToProsecutionCase.getProsecutionCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateListingNumber(updateListingNumberToProsecutionCase.getOffenceListingNumbers());

        appendEventsToStream(updateListingNumberToProsecutionCaseEnvelope, eventStream, events);
    }

    @Handles("progression.command.increase-listing-number-to-prosecution-case")
    public void handleIncreaseListingNumber(final Envelope<IncreaseListingNumberToProsecutionCase> increaseListingNumberForProsecutionCaseEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.increase-listing-number-to-prosecution-case caseId {}", increaseListingNumberForProsecutionCaseEnvelope.payload().getProsecutionCaseId());
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
