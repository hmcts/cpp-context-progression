package uk.gov.moj.cpp.progression.handler;

import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.progression.helper.CourtDocumentHelper.setDefaults;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CreateCourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S3655", "squid:S2629", "squid:S1168"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateCourtDocumentHandler {

    public static final String READ_USER_GROUPS = "readUserGroups";
    public static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateCourtDocumentHandler.class.getName());
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private Enveloper enveloper;
    @Inject
    private RefDataService refDataService;
    @Inject
    private Requester requester;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonToObjectConverter;

    @Handles("progression.command.create-court-document")
    public void handle(final Envelope<CreateCourtDocument> createCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.create-court-document {}", createCourtDocumentEnvelope);
        final CourtDocument courtDocument = setDefaults(createCourtDocumentEnvelope.payload().getCourtDocument());
        final EventStream eventStream = eventSource.getStreamById(courtDocument.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);
        final Boolean isCpsCase = createCourtDocumentEnvelope.payload().getIsCpsCase();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(createCourtDocumentEnvelope.metadata(), JsonValue.NULL);
        final Stream<Object> events = courtDocumentAggregate.createCourtDocument(enrichCourtDocument(courtDocument, jsonEnvelope), isCpsCase);

        appendEventsToStream(createCourtDocumentEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

    private CourtDocument enrichCourtDocument(final CourtDocument courtDocument, final JsonEnvelope incomingEvent) {

        if (!needToEnrichCourtDocument(courtDocument)) {
            return courtDocument;
        }
        final Optional<JsonObject> documentTypeData = refDataService.getDocumentTypeAccessData(courtDocument.getDocumentTypeId(), incomingEvent, requester);
        final JsonObject documentTypeDataJsonObject = documentTypeData.orElseThrow(() -> new RuntimeException("failed to look up nows document type " + courtDocument.getDocumentTypeId()));

        final JsonObject documentTypeRBACData = documentTypeDataJsonObject.getJsonObject(COURT_DOCUMENT_TYPE_RBAC);
        final Integer seqNum = Integer.parseInt(documentTypeDataJsonObject.getJsonNumber("seqNum") == null ? "0" : documentTypeDataJsonObject.getJsonNumber("seqNum").toString());
        final List<String> rbacUserGroups = getRBACUserGroups(documentTypeRBACData, READ_USER_GROUPS);

        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withMaterials(getCourtDocumentMaterials(courtDocument, rbacUserGroups))
                .withDocumentCategory(
                        DocumentCategory.documentCategory()
                                .withNowDocument(courtDocument.getDocumentCategory().getNowDocument())
                                .withApplicationDocument(courtDocument.getDocumentCategory().getApplicationDocument())
                                .withCaseDocument(courtDocument.getDocumentCategory().getCaseDocument())
                                .withDefendantDocument(courtDocument.getDocumentCategory().getDefendantDocument())
                                .build()
                )
                .withName(courtDocument.getName())
                .withDocumentTypeRBAC(getRBACDataObject(documentTypeRBACData))
                .withSeqNum(seqNum)
                .withAmendmentDate(courtDocument.getAmendmentDate())
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withMimeType(courtDocument.getMimeType())
                .withSendToCps(courtDocument.getSendToCps())
                .build();
    }

    private List<Material> getCourtDocumentMaterials(final CourtDocument courtDocument, final List<String> rbacUserGroups) {
        if (CollectionUtils.isEmpty(courtDocument.getMaterials())) {
            return new ArrayList<>();
        }

        List<String> permittedGroups = courtDocument.getMaterials().get(0).getUserGroups();
        if (permittedGroups == null) {
            permittedGroups = rbacUserGroups;
        }

        if (!permittedGroups.containsAll(rbacUserGroups)) {
            permittedGroups = Stream.concat(emptyIfNull(rbacUserGroups)
                    .map(el -> el.replace("\"", "")), emptyIfNull(courtDocument.getMaterials().get(0).getUserGroups()))
                    .collect(Collectors.toList());
        }

        return Arrays.asList(Material.material()
                .withId(courtDocument.getMaterials().get(0).getId())
                .withGenerationStatus(courtDocument.getMaterials().get(0).getGenerationStatus())
                .withUserGroups(permittedGroups)
                .withUploadDateTime(courtDocument.getMaterials().get(0).getUploadDateTime())
                .withName(courtDocument.getMaterials().get(0).getName())
                .build());
    }

    public Stream<String> emptyIfNull(Collection<String> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    private DocumentTypeRBAC getRBACDataObject(final JsonObject documentTypeRBACData) {
        return DocumentTypeRBAC.
                documentTypeRBAC()
                .withUploadUserGroups(getRBACUserGroups(documentTypeRBACData, UPLOAD_ACCESS))
                .withReadUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                .withDownloadUserGroups(getRBACUserGroups(documentTypeRBACData, DOWNLOAD_ACCESS))
                .withDeleteUserGroups(getRBACUserGroups(documentTypeRBACData, DELETE_ACCESS))
                .build();
    }

    private boolean needToEnrichCourtDocument(final CourtDocument courtDocument) {
        return (courtDocument.getDocumentTypeRBAC() == null || courtDocument.getSeqNum() == null);
    }

    private List<String> getRBACUserGroups(final JsonObject documentTypeRBACData, final String accessLevel) {

        if (!documentTypeRBACData.containsKey(accessLevel)) {
            return new ArrayList<>();
        }
        final JsonArray documentTypeRBACJsonArray = documentTypeRBACData.getJsonArray(accessLevel);

        return IntStream.range(0, (documentTypeRBACJsonArray).size()).mapToObj(i -> documentTypeRBACJsonArray.getJsonObject(i).getJsonObject("cppGroup").getString("groupName")).collect(toList());
    }

}
