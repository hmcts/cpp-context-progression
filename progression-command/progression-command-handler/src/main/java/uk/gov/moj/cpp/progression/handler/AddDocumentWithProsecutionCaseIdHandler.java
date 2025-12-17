package uk.gov.moj.cpp.progression.handler;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.AddDocumentWithProsecutionCaseId;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1168")
@ServiceComponent(COMMAND_HANDLER)
public class AddDocumentWithProsecutionCaseIdHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Handles("progression.command.add-document-with-prosecution-case-id")
    public void handle(final Envelope<AddDocumentWithProsecutionCaseId> addDocumentEnvelope) throws EventStreamException {
        logger.debug("progression.command.add-document-with-prosecution-case-id{}", addDocumentEnvelope);

        final CourtDocument courtDocument = addDocumentEnvelope.payload().getCourtDocument();
        final UUID caseId = addDocumentEnvelope.payload().getCaseId();

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(
                eventStream,
                CaseAggregate.class);

        final Stream<Object> events = caseAggregate.addDocument(courtDocument);

        appendEventsToStream(addDocumentEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
