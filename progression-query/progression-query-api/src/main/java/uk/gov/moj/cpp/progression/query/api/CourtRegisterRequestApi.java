package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CourtRegisterDocumentRequestQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtRegisterRequestApi {

    @Inject
    private CourtRegisterDocumentRequestQueryView courtRegisterDocumentRequestQueryView;

    @Handles("progression.query.court-register-document-request")
    public JsonEnvelope getCourtRegisterDocumentRequest(final JsonEnvelope query) {
        return courtRegisterDocumentRequestQueryView.getCourtRegisterRequests(query);
    }

    @Handles("progression.query.court-register-document-by-material")
    public JsonEnvelope getCourtRegisterDocumentRequestByMaterial(final JsonEnvelope query) {
        return courtRegisterDocumentRequestQueryView.getCourtRegisterByMaterial(query);
    }

    @Handles("progression.query.court-register-document-by-request-date")
    public JsonEnvelope getCourtRegisterDocumentByRequestDate(final JsonEnvelope query) {
        return courtRegisterDocumentRequestQueryView.getCourtRegistersByRequestDate(query);
    }
}
