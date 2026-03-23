package uk.gov.moj.cpp.progression.handler;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.UpdateCourtDocument;
import uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.command.UpdateSendToCpsFlag;
import uk.gov.moj.cpp.progression.common.CourtDocumentMetadata;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1168")
@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCourtDocumentHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCourtDocumentHandler.class.getName());
    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";
    public static final String SECTION = "section";
    public static final String SEQNUM = "seqNum";
    public static final String CASE_LEVEL = "Case level";
    public static final String DEFENDANT_LEVEL = "Defendant level";
    public static final String DOCUMENT_CATEGORY = "documentCategory";
    public static final String APPLICATIONS = "Applications";

    @Inject
    private EnvelopeHelper envelopeHelper;
    @Inject
    private RefDataService refDataService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private Requester requester;

    @Handles("progression.command.update-court-document")
    public void handleUpdateCourtDocument(final Envelope<UpdateCourtDocument> updateCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-court-document {}", updateCourtDocumentEnvelope);
        final UpdateCourtDocument updateCourtDocument = updateCourtDocumentEnvelope.payload();
        final CourtDocumentMetadata courtDocumentMetadata = updateCourtDocumentEnvelope.payload().getCourtDocumentMetadata();
        final EventStream eventStream = eventSource.getStreamById(updateCourtDocument.getCourtDocumentId());

        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(
                JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(updateCourtDocumentEnvelope.metadata()).withName("progression.command.update-court-document"), objectToJsonObjectConverter.convert(updateCourtDocument)));

        final JsonObject documentTypeData = refDataService.getDocumentTypeAccessData(updateCourtDocument.getDocumentTypeId(), jsonEnvelope, requester).orElseThrow(() -> new RuntimeException("Reference Data not found "));

        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);

        Stream<Object> events = null;

        final DocumentCategory documentCategory = buildDocumentCategory(updateCourtDocument, documentTypeData.getString(DOCUMENT_CATEGORY));

        if (documentCategory != null) {
            final CourtDocument inputCourtDocumentDetails = CourtDocument.courtDocument().
                    withCourtDocumentId(updateCourtDocument.getCourtDocumentId()).
                    withDocumentTypeId(updateCourtDocument.getDocumentTypeId()).
                    withDocumentCategory(documentCategory).
                    withCourtDocumentId(updateCourtDocument.getCourtDocumentId()).
                    withContainsFinancialMeans(updateCourtDocument.getContainsFinancialMeans()).
                    withDocumentTypeDescription(documentTypeData.getString(SECTION)).
                    withName(updateCourtDocument.getName()).
                    withSeqNum(valueOf(documentTypeData.getInt(SEQNUM))).
                    withSendToCps(updateCourtDocument.getSendToCps() != null && updateCourtDocument.getSendToCps())
                    .build();
            final List<UUID> petFormFinalisedDocuments= new ArrayList<>();
            final List<UUID> bcmFormFinalisedDocuments= new ArrayList<>();
            final List<UUID> ptphFormFinalisedDocuments= new ArrayList<>();
            if (inputCourtDocumentDetails.getSendToCps()) {
                final UUID caseId = updateCourtDocument.getProsecutionCaseId();
                if(nonNull(caseId)) {
                    final EventStream caseEventStream = eventSource.getStreamById(caseId);
                    final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);
                    petFormFinalisedDocuments.addAll(caseAggregate.getPetFormFinalisedDocuments());
                    bcmFormFinalisedDocuments.addAll(caseAggregate.getBcmFormFinalisedDocuments());
                    ptphFormFinalisedDocuments.addAll(caseAggregate.getPtphFormFinalisedDocuments());
                }
            }
            events = courtDocumentAggregate
                    .updateCourtDocument(inputCourtDocumentDetails,
                            updateCourtDocument.getReceivedDateTime(),
                            buildDocumentTypeRBAC(documentTypeData),
                            petFormFinalisedDocuments,
                            bcmFormFinalisedDocuments,
                            ptphFormFinalisedDocuments ,
                            courtDocumentMetadata);
        } else {
            events = courtDocumentAggregate.updateCourtDocumentFailed(updateCourtDocument.getCourtDocumentId(), format("Update document is not supported for this Document Category %s", documentTypeData.getString(DOCUMENT_CATEGORY)));
        }

        appendEventsToStream(updateCourtDocumentEnvelope, eventStream, events);
    }

    @Handles("progression.command.update-court-document-print-time")
    public void handleUpdateCourtDocumentPrintTime(final Envelope<UpdateCourtDocumentPrintTime> updateCourtDocumentPrintTimeEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-court-document-print-time {}", updateCourtDocumentPrintTimeEnvelope);
        final UpdateCourtDocumentPrintTime payload = updateCourtDocumentPrintTimeEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(payload.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);

        final Stream<Object> events = courtDocumentAggregate.updateCourtDocumentPrintTime(payload.getMaterialId(), payload.getCourtDocumentId(), payload.getPrintedAt());
        appendEventsToStream(updateCourtDocumentPrintTimeEnvelope, eventStream, events);
    }

    private DocumentCategory buildDocumentCategory(final UpdateCourtDocument updateCourtDocument, final String documentCategory) {
        if (documentCategory.equalsIgnoreCase(CASE_LEVEL)) {
            return DocumentCategory.documentCategory().
                    withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(updateCourtDocument.getProsecutionCaseId()).build())
                    .build();
        } else if (documentCategory.equalsIgnoreCase(DEFENDANT_LEVEL)) {
            return DocumentCategory.documentCategory().
                    withDefendantDocument(DefendantDocument.defendantDocument().
                            withDefendants(updateCourtDocument.getDefendants()).
                            withProsecutionCaseId(updateCourtDocument.getProsecutionCaseId()).build()).build();
        } else if (documentCategory.equalsIgnoreCase(APPLICATIONS)) {
            return DocumentCategory.documentCategory()
                    .withApplicationDocument(ApplicationDocument.applicationDocument()
                            .withApplicationId(updateCourtDocument.getApplicationId())
                            .build()).build();
        }

        return null;
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private DocumentTypeRBAC buildDocumentTypeRBAC(final JsonObject documentTypeRBACData) {
        if (null != documentTypeRBACData && null != documentTypeRBACData.getJsonObject("courtDocumentTypeRBAC")) {

            final JsonObject courtDocumentTypeRBAC = documentTypeRBACData.getJsonObject("courtDocumentTypeRBAC");
            return DocumentTypeRBAC.
                    documentTypeRBAC()
                    .withUploadUserGroups(getRBACUserGroups(courtDocumentTypeRBAC, UPLOAD_ACCESS))
                    .withReadUserGroups(getRBACUserGroups(courtDocumentTypeRBAC, READ_ACCESS))
                    .withDownloadUserGroups(getRBACUserGroups(courtDocumentTypeRBAC, DOWNLOAD_ACCESS))
                    .withDeleteUserGroups(getRBACUserGroups(courtDocumentTypeRBAC, DELETE_ACCESS))
                    .build();
        }
        LOGGER.error("Unable to process as ref data for document Type is not present");
        return null;
    }

    private List<String> getRBACUserGroups(final JsonObject documentTypeData, final String accessLevel) {
        final JsonArray documentTypeRBACJsonArray = documentTypeData.getJsonArray(accessLevel);
        if (null == documentTypeRBACJsonArray || documentTypeRBACJsonArray.isEmpty()) {
            return null;
        }
        return IntStream.range(0, (documentTypeRBACJsonArray).size()).mapToObj(i -> documentTypeRBACJsonArray.getJsonObject(i).getJsonObject("cppGroup").getString("groupName")).collect(toList());
    }

    @Handles("progression.command.update-send-to-cps-flag")
    public void handleUpdateSendToCpsFlag(final Envelope<UpdateSendToCpsFlag> updateSendToCpsFlagEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-send-to-cps-flag {}", updateSendToCpsFlagEnvelope.payload());

        final UpdateSendToCpsFlag updateSendToCpsFlag = updateSendToCpsFlagEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateSendToCpsFlag.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);

        final Stream<Object> events = courtDocumentAggregate.updateSendToCpsFlag(updateSendToCpsFlag.getCourtDocumentId(),
                nonNull(updateSendToCpsFlag.getSendToCps())?updateSendToCpsFlag.getSendToCps():false,
                updateSendToCpsFlag.getCourtDocument());
        appendEventsToStream(updateSendToCpsFlagEnvelope, eventStream, events);

    }
}
