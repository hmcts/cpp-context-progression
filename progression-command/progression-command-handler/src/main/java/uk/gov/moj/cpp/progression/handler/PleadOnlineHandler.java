package uk.gov.moj.cpp.progression.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.HandleOnlinePleaDocumentCreation;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnlinePcqVisited;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class PleadOnlineHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleadOnlineHandler.class);

    @Inject
    EventSource eventSource;

    @Inject
    AggregateService aggregateService;


    @Handles("progression.command.plead-online")
    public void handlePleadOnlineRequest(final Envelope<PleadOnline> envelope) throws EventStreamException {
        final PleadOnline pleadOnline = envelope.payload();

        LOGGER.info("progression.command.plead-online with caseId={} for defendantId={}", pleadOnline.getCaseId(), pleadOnline.getDefendantId());

        final EventStream eventStream = eventSource.getStreamById(pleadOnline.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.recordOnlinePlea(pleadOnline);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.handle-online-plea-document-creation")
    public void handleNotifyForOnlinePlea(final Envelope<HandleOnlinePleaDocumentCreation> envelope) throws EventStreamException {

        final HandleOnlinePleaDocumentCreation handleOnlinePleaDocumentCreation = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(handleOnlinePleaDocumentCreation.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = caseAggregate.handleOnlinePleaDocumentCreation(handleOnlinePleaDocumentCreation);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));

    }

    @Handles("progression.command.plead-online-pcq-visited")
    public void handlePleadOnlinePcqVisitedRequest(final Envelope<PleadOnlinePcqVisited> envelope) throws EventStreamException {
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = envelope.payload();
        LOGGER.info("progression.command.plead-online-pcq-visited with caseId={} for defendantId={}", pleadOnlinePcqVisited.getCaseId(), pleadOnlinePcqVisited.getDefendantId());

        final EventStream eventStream = eventSource.getStreamById(pleadOnlinePcqVisited.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.createOnlinePleaPcqVisited(pleadOnlinePcqVisited);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }
}
