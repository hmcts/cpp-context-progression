package uk.gov.moj.cpp.progression.handler;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.moj.cpp.progression.helper.CourtDocumentHelper.setDefaults;

import uk.gov.justice.core.courts.AddCourtDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.exception.RefDataDefinitionException;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1168")
@ServiceComponent(Component.COMMAND_HANDLER)
public class AddCourtDocumentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddCourtDocumentHandler.class.getName());
    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;

    @Inject
    private Requester requester;
    @Inject
    private EnvelopeHelper envelopeHelper;
    @Inject
    private ReferenceDataService refDataService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonToObjectConverter;

    @Handles("progression.command.add-court-document")
    public void handle(final Envelope<AddCourtDocument> addCourtDocumentEnvelope) throws RefDataDefinitionException, EventStreamException {
        LOGGER.debug("progression.command.add-court-document {}", addCourtDocumentEnvelope);
        final CourtDocument courtDocument = setDefaults(addCourtDocumentEnvelope.payload().getCourtDocument());

        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(
                JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(addCourtDocumentEnvelope.metadata()).withName("progression.command.add-court-document"), objectToJsonObjectConverter.convert(courtDocument)));

        final JsonObject documentTypeData = refDataService.getDocumentTypeAccessData(courtDocument.getDocumentTypeId(), jsonEnvelope, requester).orElse(null);

        if (null != documentTypeData && null != documentTypeData.getJsonObject("courtDocumentTypeRBAC")) {
            enrichRefDataAndAppendEventsToStream(addCourtDocumentEnvelope, courtDocument, documentTypeData);
        } else {
            throw new RefDataDefinitionException("Unable to process as ref data for document Type is not present");
        }
    }

    private void enrichRefDataAndAppendEventsToStream(final Envelope<AddCourtDocument> addCourtDocumentEnvelope, CourtDocument courtDocument, final JsonObject documentTypeData) throws EventStreamException {

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("courtDocument", objectToJsonObjectConverter
                        .convert(buildCourtDocumentWithMaterialUserGroups(courtDocument, documentTypeData))).build();
        LOGGER.info("court document is being created '{}' ", jsonObject);
        courtDocument = jsonToObjectConverter.convert(jsonObject.getJsonObject("courtDocument"), CourtDocument.class);


        final EventStream eventStream = eventSource.getStreamById(courtDocument.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);
        final Stream<Object> events = courtDocumentAggregate.addCourtDocument(courtDocument);
        appendEventsToStream(addCourtDocumentEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }


    private CourtDocument buildCourtDocumentWithMaterialUserGroups(final CourtDocument courtDocument, final JsonObject documentTypeData) {

        final JsonObject documentTypeRBACData = documentTypeData.getJsonObject("courtDocumentTypeRBAC");
        final Integer seqNum = Integer.parseInt(documentTypeData.getJsonNumber("seqNum")==null ? "0" : documentTypeData.getJsonNumber("seqNum").toString());

        final List<Material> materials = courtDocument.getMaterials().stream()
                .map(material -> enrichMaterial(material, documentTypeRBACData)).collect(Collectors.toList());

        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withName(courtDocument.getName())
                .withMimeType(courtDocument.getMimeType())
                .withMaterials(materials)
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withSeqNum(seqNum)
                .withDocumentTypeRBAC(DocumentTypeRBAC.
                        documentTypeRBAC()
                        .withUploadUserGroups(getRBACUserGroups(documentTypeRBACData, UPLOAD_ACCESS))
                        .withReadUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                        .withDownloadUserGroups(getRBACUserGroups(documentTypeRBACData, DOWNLOAD_ACCESS))
                        .withDeleteUserGroups(getRBACUserGroups(documentTypeRBACData, DELETE_ACCESS))
                        .build())
                .build();
    }

    private Material enrichMaterial(Material material, final JsonObject documentTypeRBACData){
        return Material.material()
                .withId(material.getId())
                .withGenerationStatus(material.getGenerationStatus())
                .withName(material.getName())
                .withUploadDateTime(material.getUploadDateTime()!=null ? material.getUploadDateTime() : ZonedDateTime.now(ZoneOffset.UTC))
                .withReceivedDateTime(material.getReceivedDateTime())
                .withUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                .build();
    }

    private List<String> getRBACUserGroups(final JsonObject documentTypeData, final String accessLevel) {

        final JsonArray documentTypeRBACJsonArray = documentTypeData.getJsonArray(accessLevel);
        if (null == documentTypeRBACJsonArray || documentTypeRBACJsonArray.isEmpty()) {
            return null;
        }


        return IntStream.range(0, (documentTypeRBACJsonArray).size()).mapToObj(i -> documentTypeRBACJsonArray.getJsonObject(i).getJsonObject("cppGroup").getString("groupName")).collect(toList());

    }

}
