package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.progression.courts.ReapplyMediaReportingRestrictions;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ReApplyMediaReportingRestrictionOnCasesHandler  extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReApplyMediaReportingRestrictionOnCasesHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;


    @Handles("progression.command.handler.reapply-media-reporting-restrictions")
    public void handle(final Envelope<ReapplyMediaReportingRestrictions> reapplyMediaReportingRestrictionsEnvelope) {

        LOGGER.info("progression.command.handler.reapply-media-reporting-restrictions {}", reapplyMediaReportingRestrictionsEnvelope.payload());
        final ReapplyMediaReportingRestrictions reapplyMediaReportingRestrictions = reapplyMediaReportingRestrictionsEnvelope.payload();
        final UUID caseId = reapplyMediaReportingRestrictions.getCaseId();
        applyEventOnAggregate(reapplyMediaReportingRestrictionsEnvelope, caseId);

    }

    private void applyEventOnAggregate(final Envelope<ReapplyMediaReportingRestrictions> reapplyMediaReportingRestrictionsEnvelope, final UUID caseId) {
        try {
            final EventStream eventStream = eventSource.getStreamById(caseId);

            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final Stream<Object> events = caseAggregate.reApplyMiReportingRestrictions(caseId);

            appendEventsToStream(reapplyMediaReportingRestrictionsEnvelope, eventStream, events);
        } catch (EventStreamException e) {
            LOGGER.info("progression.command.handler.reapply-media-reporting-restrictions stream exception {}", e);
        }
    }
}
