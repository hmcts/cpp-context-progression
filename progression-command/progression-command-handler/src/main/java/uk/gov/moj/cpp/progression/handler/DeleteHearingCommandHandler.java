package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.progression.courts.DeleteHearing;
import uk.gov.justice.progression.courts.DeleteHearingForCourtApplication;
import uk.gov.justice.progression.courts.DeleteHearingForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class DeleteHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteHearingCommandHandler.class);

    private static final String PROGRESSION_COMMAND_DELETE_HEARING = "progression.command.delete-hearing";
    private static final String PROGRESSION_COMMAND_DELETE_HEARING_FOR_PROSECUTION_CASE = "progression.command.delete-hearing-for-prosecution-case";
    private static final String PROGRESSION_COMMAND_DELETE_HEARING_FOR_COURT_APPLICATION = "progression.command.delete-hearing-for-court-application";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(PROGRESSION_COMMAND_DELETE_HEARING)
    public void handleDeleteHearing(final Envelope<DeleteHearing> deleteHearingEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'{}' received with payload {}", PROGRESSION_COMMAND_DELETE_HEARING, deleteHearingEnvelope);
        }

        final DeleteHearing deleteHearing = deleteHearingEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(deleteHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.deleteHearing(deleteHearing.getHearingId());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(deleteHearingEnvelope)));
    }

    @Handles(PROGRESSION_COMMAND_DELETE_HEARING_FOR_PROSECUTION_CASE)
    public void handleDeleteHearingForProsecutionCase(final Envelope<DeleteHearingForProsecutionCase> deleteHearingForCaseEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'{}' received with payload {}", PROGRESSION_COMMAND_DELETE_HEARING_FOR_PROSECUTION_CASE, deleteHearingForCaseEnvelope);
        }
        final DeleteHearingForProsecutionCase deleteHearingForProsecutionCase = deleteHearingForCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(deleteHearingForProsecutionCase.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.deleteHearingRelatedToProsecutionCase(deleteHearingForProsecutionCase.getHearingId(), deleteHearingForProsecutionCase.getProsecutionCaseId());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(deleteHearingForCaseEnvelope)));
    }

    @Handles(PROGRESSION_COMMAND_DELETE_HEARING_FOR_COURT_APPLICATION)
    public void handleDeleteHearingForCourtApplication(final Envelope<DeleteHearingForCourtApplication> deleteHearingForCourtApplicationEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'{}' received with payload {}", PROGRESSION_COMMAND_DELETE_HEARING_FOR_COURT_APPLICATION, deleteHearingForCourtApplicationEnvelope);
        }
        final DeleteHearingForCourtApplication deleteHearingForCourtApplication = deleteHearingForCourtApplicationEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(deleteHearingForCourtApplication.getCourtApplicationId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.deleteHearingRelatedToCourtApplication(deleteHearingForCourtApplication.getHearingId(), deleteHearingForCourtApplication.getCourtApplicationId());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(deleteHearingForCourtApplicationEnvelope)));
    }

}
