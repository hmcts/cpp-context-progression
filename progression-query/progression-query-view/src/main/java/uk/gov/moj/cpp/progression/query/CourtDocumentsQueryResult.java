package uk.gov.moj.cpp.progression.query;

import uk.gov.justice.core.courts.CourtDocument;

import java.util.List;

@SuppressWarnings("squid:S2384")
public class CourtDocumentsQueryResult {
    private List<CourtDocument> documentIndices;

    public List<CourtDocument> getDocumentIndices() {
        return documentIndices;
    }

    public void setDocumentIndices(List<CourtDocument> documentIndices) {
        this.documentIndices = documentIndices;
    }
}
