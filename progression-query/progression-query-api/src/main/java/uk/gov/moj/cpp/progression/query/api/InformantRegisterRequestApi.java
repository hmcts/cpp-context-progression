package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class InformantRegisterRequestApi {
    @Inject
    private Requester requester;

    @Handles("progression.query.informant-register-document-request")
    public JsonEnvelope getInformantRegisterDocumentRequest(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.informant-register-document-by-material")
    public JsonEnvelope getInformantRegisterDocumentRequestByMaterial(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.informant-register-document-by-request-date")
    public JsonEnvelope getInformantRegisterDocumentByRequestDate(final JsonEnvelope query) {
        return requester.request(query);
    }
}
