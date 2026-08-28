package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.RecordNowsDocumentFailed;
import uk.gov.justice.core.courts.RecordNowsDocumentSent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.progression.courts.RecordNowsDocumentGenerated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NowDocumentRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NowDocumentRequestHandler.class.getName());

    private static final String PROGRESSION_COMMAND_REQUEST_NOW_DOCUMENT = "progression.command.add-now-document-request";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(PROGRESSION_COMMAND_REQUEST_NOW_DOCUMENT)
    public void handleAddNowDocumentRequest(final Envelope<NowDocumentRequest> envelope) throws EventStreamException {
        final NowDocumentRequest nowDocumentRequest = envelope.payload();
        final UUID materialId = nowDocumentRequest.getMaterialId();
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        if (isBulkCivilCase(nowDocumentRequest)) {
            LOGGER.info("Skipping NOW/EDT document generation for bulk civil case, materialId {}", materialId);
            return;
        }

        final EventStream eventStream = eventSource.getStreamById(nowDocumentRequest.getMaterialId());

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.createNowDocumentRequest(materialId, nowDocumentRequest, userId);

        appendEventsToStream(envelope, eventStream, events);
    }

    /**
     * A bulk civil case (isCivil combined with isGroupMaster/isGroupMember — never the group
     * flags alone) does not generate or dispatch NOW/EDT/hearing-notice documents; the entered
     * result is still stored via the normal hearing-result handling, unaffected by this check.
     */
    private boolean isBulkCivilCase(final NowDocumentRequest nowDocumentRequest) {
        if (isNull(nowDocumentRequest.getCases()) || nowDocumentRequest.getCases().isEmpty()) {
            return false;
        }
        final UUID caseId = nowDocumentRequest.getCases().get(0);
        final EventStream caseEventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);
        final ProsecutionCase prosecutionCase = caseAggregate.getProsecutionCase();
        if (isNull(prosecutionCase)) {
            return false;
        }
        final boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();
        final boolean isGroupMaster = nonNull(prosecutionCase.getIsGroupMaster()) && prosecutionCase.getIsGroupMaster();
        final boolean isGroupMember = nonNull(prosecutionCase.getIsGroupMember()) && prosecutionCase.getIsGroupMember();
        return isCivil && (isGroupMaster || isGroupMember);
    }

    @Handles("progression.command.record-nows-document-sent")
    public void recordNowsDocumentSent(final Envelope<RecordNowsDocumentSent> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-nows-document-sent {}", envelope.payload());

        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final RecordNowsDocumentSent recordNowsDocumentSent = envelope.payload();

        final UUID materialId = recordNowsDocumentSent.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowsDocumentSent(materialId, userId, recordNowsDocumentSent);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.record-nows-document-generated")
    public void recordNowsDocumentGenerated(final Envelope<RecordNowsDocumentGenerated> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-nows-document-generated {}", envelope.payload());

        final RecordNowsDocumentGenerated recordNowsDocumentGenerated = envelope.payload();

        final UUID materialId = recordNowsDocumentGenerated.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowsDocumentGenerated(materialId, recordNowsDocumentGenerated);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.record-nows-document-failed")
    public void recordNowsDocumentFailed(final Envelope<RecordNowsDocumentFailed> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-nows-document-failed {}", envelope.payload());

        final RecordNowsDocumentFailed recordNowsDocumentFailed = envelope.payload();

        final UUID materialId = recordNowsDocumentFailed.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowsDocumentFailed(materialId, recordNowsDocumentFailed);

        appendEventsToStream(envelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}

