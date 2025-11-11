package uk.gov.moj.cpp.progression.handler;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.FeeAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(COMMAND_HANDLER)
public class InitiateCourtProceedingsForGroupCasesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateCourtProceedingsForGroupCasesHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.initiate-court-proceedings-for-group-cases")
    public void handle(final Envelope<InitiateCourtProceedingsForGroupCases> initiateCourtProceedingsForGroupCasesEnvelope) throws EventStreamException {

        LOGGER.info("progression.command.initiate-court-proceedings-for-group-cases.json {}", initiateCourtProceedingsForGroupCasesEnvelope.payload());
        final InitiateCourtProceedingsForGroupCases command = initiateCourtProceedingsForGroupCasesEnvelope.payload();
        final List<ProsecutionCase> prosecutionCases = command.getCourtReferral().getProsecutionCases();
        final Optional<ProsecutionCase> prosecutionCase = verifyCaseCreationEvent(prosecutionCases);
        if (prosecutionCase.isPresent()) {
            raiseGroupCaseExists(command.getGroupId(), prosecutionCase.get(), initiateCourtProceedingsForGroupCasesEnvelope);
        } else {
            raiseCaseCreationEvent(prosecutionCases, initiateCourtProceedingsForGroupCasesEnvelope);
            raiseCourtProceedingsInitiatedEvent(command.getGroupId(), command.getCourtReferral(), initiateCourtProceedingsForGroupCasesEnvelope);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private void raiseCaseCreationEvent(final List<ProsecutionCase> prosecutionCases, final Envelope<InitiateCourtProceedingsForGroupCases> envelope) throws EventStreamException {

        final List<CivilFees> commonCivilFeesForGroup = prosecutionCases.get(0).getCivilFees();
        //for group cases all fee data should be same for each group member case. Ref: DD-35774 -> AC-4
        if(isNotEmpty(commonCivilFeesForGroup)) {
            for (CivilFees civilFee : commonCivilFeesForGroup) {
                final EventStream feeEventStream = eventSource.getStreamById(civilFee.getFeeId());
                final FeeAggregate feeAggregate = aggregateService.get(feeEventStream, FeeAggregate.class);
                final Stream<Object> feeEvents = feeAggregate.addCivilFee(civilFee);
                appendEventsToStream(envelope, feeEventStream, feeEvents);
            }
        }

        for(final ProsecutionCase prosecutionCase: prosecutionCases) {
            final EventStream eventStream = eventSource.getStreamById(prosecutionCase.getId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

            final Stream<Object> events = caseAggregate.createProsecutionCase(prosecutionCase, commonCivilFeesForGroup);
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private Optional<ProsecutionCase> verifyCaseCreationEvent(final List<ProsecutionCase> prosecutionCases) {
        for (final ProsecutionCase prosecutionCase : prosecutionCases) {
            final long streamSize = this.eventSource.getStreamById(prosecutionCase.getId()).size();
            if (streamSize > 0) {
                return Optional.of(prosecutionCase);
            }
        }
        return Optional.empty();
    }

    private void raiseCourtProceedingsInitiatedEvent(final UUID groupId, final CourtReferral courtReferral, final Envelope<InitiateCourtProceedingsForGroupCases> envelope) throws EventStreamException {
        final EventStream stream = this.eventSource.getStreamById(groupId);
        final GroupCaseAggregate aggregate = this.aggregateService.get(stream, GroupCaseAggregate.class);
        final Stream<Object> events = aggregate.initiateCourtProceedings(courtReferral);
        appendEventsToStream(envelope, stream, events);
    }


    private void raiseGroupCaseExists(final UUID groupId, final ProsecutionCase prosecutionCase, final Envelope<InitiateCourtProceedingsForGroupCases> envelope) throws EventStreamException {
        final EventStream stream = this.eventSource.getStreamById(groupId);
        final GroupCaseAggregate aggregate = this.aggregateService.get(stream, GroupCaseAggregate.class);
        final Stream<Object> events = aggregate.raiseGroupCaseExists(groupId, prosecutionCase);
        appendEventsToStream(envelope, stream, events);
    }
}
