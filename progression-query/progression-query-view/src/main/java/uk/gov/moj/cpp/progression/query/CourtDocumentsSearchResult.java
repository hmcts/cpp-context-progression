package uk.gov.moj.cpp.progression.query;

import uk.gov.justice.core.courts.CourtDocumentIndex;

import java.util.List;

@SuppressWarnings("squid:S2384")
public class CourtDocumentsSearchResult {
    private List<CourtDocumentIndex> documentIndices;

    public List<CourtDocumentIndex> getDocumentIndices() {
        return documentIndices;
    }

    public void setDocumentIndices(List<CourtDocumentIndex> documentIndices) {
        this.documentIndices = documentIndices;
    }

}
