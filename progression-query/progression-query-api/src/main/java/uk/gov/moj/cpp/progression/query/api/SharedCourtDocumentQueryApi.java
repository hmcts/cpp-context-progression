package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.SharedCourtDocumentsQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class SharedCourtDocumentQueryApi {

    @Inject
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @Handles("progression.query.application.shared-court-documents-links")
    public JsonEnvelope getApplicationSharedCourtDocumentsLinks(final JsonEnvelope query) {
        return sharedCourtDocumentsQueryView.getApplicationSharedCourtDocumentsLinks(query);

    }
}
