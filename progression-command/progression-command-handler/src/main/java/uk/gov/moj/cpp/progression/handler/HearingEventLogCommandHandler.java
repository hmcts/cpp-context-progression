package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.progression.courts.AaagHearingEventLogsDocumentCreated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.api.CreateHearingEventLogDocument;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1168")
@ServiceComponent(COMMAND_HANDLER)
public class HearingEventLogCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventLogCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Handles("progression.command.create-hearing-event-log-document")
    public void handle(final Envelope<CreateHearingEventLogDocument> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.create-hearing-event-log-document payload: {}", envelope.payload());
        }
        final CreateHearingEventLogDocument createHearingEventLogDocument = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(createHearingEventLogDocument.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.getHearingEventLogsDocuments(createHearingEventLogDocument.getCaseId(), Optional.empty());
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.create-aaag-hearing-event-log-document")
    public void handleAaag(final Envelope<AaagHearingEventLogsDocumentCreated> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.create-aaag-hearing-event-log-document payload: {}", envelope.payload());
        }
        final AaagHearingEventLogsDocumentCreated aaagHearingEventLogsDocumentCreated = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(aaagHearingEventLogsDocumentCreated.getApplicationId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.getHearingEventLogsDocuments(aaagHearingEventLogsDocumentCreated.getCaseId(), Optional.of(aaagHearingEventLogsDocumentCreated.getApplicationId()));
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }
}
