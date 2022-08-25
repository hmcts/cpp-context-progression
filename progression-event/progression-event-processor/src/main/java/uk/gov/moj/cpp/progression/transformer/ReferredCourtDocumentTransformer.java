package uk.gov.moj.cpp.progression.transformer;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings("squid:S1168")
public class ReferredCourtDocumentTransformer {

    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private RefDataService referenceDataService;

    public CourtDocument transform(final ReferredCourtDocument referredCourtDocument, final JsonEnvelope jsonEnvelope) {
        final JsonObject documentTypeDataJson = referenceDataService
                .getDocumentTypeAccessData(referredCourtDocument.getDocumentTypeId(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("section", referredCourtDocument.getDocumentTypeId().toString()));

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withMaterials(referredCourtDocument.getMaterials())
                .withCourtDocumentId(referredCourtDocument.getCourtDocumentId())
                .withDocumentCategory(referredCourtDocument.getDocumentCategory())
                .withDocumentTypeId(referredCourtDocument.getDocumentTypeId())
                .withName(referredCourtDocument.getName())
                .withMimeType(referredCourtDocument.getMimeType())
                .withContainsFinancialMeans(referredCourtDocument.getContainsFinancialMeans())
                .build();

        return buildCourtDocumentWithMaterialUserGroups(courtDocument,documentTypeDataJson);
    }

    private CourtDocument buildCourtDocumentWithMaterialUserGroups(final CourtDocument courtDocument, final JsonObject documentTypeData) {

        final JsonObject documentTypeRBACData = documentTypeData.getJsonObject("courtDocumentTypeRBAC");
        final Integer seqNum = Integer.parseInt(documentTypeData.getJsonNumber("seqNum")==null ? "0" : documentTypeData.getJsonNumber("seqNum").toString());

        final List<Material> materials = courtDocument.getMaterials().stream()
                .map(material -> enrichMaterial(material, documentTypeRBACData)).collect(Collectors.toList());

        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withDocumentTypeDescription(documentTypeData.getString("section"))
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
