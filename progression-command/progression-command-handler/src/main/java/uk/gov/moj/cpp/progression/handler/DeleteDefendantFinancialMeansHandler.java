package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.DeleteFinancialMeans;
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

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DeleteDefendantFinancialMeansHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDefendantFinancialMeansHandler.class.getName());
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.delete-financial-means")
    public void handle(final Envelope<DeleteFinancialMeans> deleteFinancialMeansEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.delete-financial-means {}", deleteFinancialMeansEnvelope.payload());

        final DeleteFinancialMeans deleteFinancialMeans = deleteFinancialMeansEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(deleteFinancialMeans.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.deleteFinancialMeansData(deleteFinancialMeans.getDefendantId(), deleteFinancialMeans.getCaseId());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(deleteFinancialMeansEnvelope)));
    }

}
