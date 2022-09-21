package uk.gov.moj.cpp.progression.handler.courts.document;

import static uk.gov.justice.core.courts.CourtDocument.courtDocument;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.List;

import javax.inject.Inject;

public class CourtDocumentEnricher {

    @Inject
    private DocumentTypeRBACFactory documentTypeRBACFactory;

    @Inject
    private EnrichedMaterialsProvider enrichedMaterialsProvider;

    public CourtDocument enrichWithMaterialUserGroups(final CourtDocument courtDocument, final DocumentTypeAccess documentTypeData) {

        final List<Material> materials = enrichedMaterialsProvider.getEnrichedMaterials(courtDocument, documentTypeData);
        final DocumentTypeRBAC documentTypeRBAC = documentTypeRBACFactory.createFromMaterialUserGroups(documentTypeData);
        final Integer seqNum = getSeqNum(documentTypeData);

        
        return courtDocument()
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withName(courtDocument.getName())
                .withMimeType(courtDocument.getMimeType())
                .withMaterials(materials)
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withSeqNum(seqNum)
                .withDocumentTypeRBAC(documentTypeRBAC)
                .withSendToCps(courtDocument.getSendToCps())
                .withNotificationType(courtDocument.getNotificationType())
                .build();
    }

    private Integer getSeqNum(final DocumentTypeAccess documentTypeData) {
        final Integer seqNum = documentTypeData.getSeqNum();

        if(seqNum != null) {
            return seqNum;
        }

        return 0;
    }
}
