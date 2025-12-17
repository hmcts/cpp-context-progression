package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.PrisonCourtRegisterDocumentRequestQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class PrisonCourtRegisterRequestApi {

    @Inject
    private PrisonCourtRegisterDocumentRequestQueryView prisonCourtRegisterDocumentRequestQueryView;

    @Handles("progression.query.prison-court-register-document-by-court-centre")
    public JsonEnvelope getPrisonCourtRegisterDocumentRequestByCourtCentre(final JsonEnvelope query) {
        return prisonCourtRegisterDocumentRequestQueryView.getPrisonCourtRegistersByCourtCentre(query);
    }
}
