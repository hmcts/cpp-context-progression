package uk.gov.moj.cpp.progression.handler.courts.document;

import static uk.gov.moj.cpp.progression.helper.CourtDocumentHelper.setDefaults;

import uk.gov.justice.core.courts.CourtDocument;

public class DefaultCourtDocumentFactory {

    public CourtDocument createDefaultCourtDocument(final CourtDocument courtDocument) {
        return setDefaults(courtDocument);
    }
}
