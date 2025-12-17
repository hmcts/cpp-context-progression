package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.List;

import javax.inject.Inject;

public class EnrichedMaterialsProvider {

    @Inject
    private MaterialEnricher materialEnricher;

    public List<Material> getEnrichedMaterials(final CourtDocument courtDocument, final DocumentTypeAccess documentTypeAccess) {

        return courtDocument.getMaterials().stream()
                .map(material -> materialEnricher.enrichMaterial(material, documentTypeAccess))
                .collect(toList());
    }
}
