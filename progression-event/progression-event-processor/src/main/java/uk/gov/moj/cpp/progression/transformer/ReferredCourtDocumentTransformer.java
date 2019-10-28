package uk.gov.moj.cpp.progression.transformer;

import static java.lang.Boolean.FALSE;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferredCourtDocumentTransformer {

    @Inject
    private ReferenceDataService referenceDataService;

    public CourtDocument transform(final ReferredCourtDocument referredCourtDocument, final JsonEnvelope jsonEnvelope) {
        final JsonObject documentTypeDataJson = referenceDataService
                .getDocumentTypeData(referredCourtDocument.getDocumentTypeId(), jsonEnvelope)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Document Type", referredCourtDocument.getDocumentTypeId().toString()));

        return CourtDocument.courtDocument()
                .withMaterials(referredCourtDocument.getMaterials())
                .withCourtDocumentId(referredCourtDocument.getCourtDocumentId())
                .withDocumentCategory(referredCourtDocument.getDocumentCategory())
                .withDocumentTypeDescription(documentTypeDataJson.getString("documentType"))
                .withDocumentTypeId(referredCourtDocument.getDocumentTypeId())
                .withName(referredCourtDocument.getName())
                .withIsRemoved(FALSE)
                .withMimeType(referredCourtDocument.getMimeType())
                .withContainsFinancialMeans(referredCourtDocument.getContainsFinancialMeans())
                .build();
    }


}
