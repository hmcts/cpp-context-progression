package uk.gov.moj.cpp.progression.query.api;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtDocumentQueryApi {

    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";
    public static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";

    @Inject
    private Requester requester;

    @Handles(COURT_DOCUMENT_SEARCH_NAME)
    public JsonEnvelope getCourtDocument(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME)
    public JsonEnvelope searchCourtDocuments(final JsonEnvelope query) {
        return requester.request(query);
    }


}
