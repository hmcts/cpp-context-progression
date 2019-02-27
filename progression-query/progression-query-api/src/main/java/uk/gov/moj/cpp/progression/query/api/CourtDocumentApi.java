package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtDocumentApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;


    @Handles("progression.query.material-metadata")
    public JsonEnvelope getCaseDocumentMetadata(final JsonEnvelope query) {
        return requester.requestAsAdmin(enveloper.withMetadataFrom(query, "material.query.material-metadata").apply(query.payloadAsJsonObject()));

    }

    /**
     * Handler returns document details and not document content. This is consequence of non
     * framework endpoint which uses standard framework interceptors. Handler is invoked at the end
     * of programmatically invoked interceptor chain, see DefaultQueryApiCasesCaseIdDocumentsDocumentIdContentResource.
     */
    @Handles("progression.query.material-content")
    public JsonEnvelope getCaseDocumentDetails(final JsonEnvelope envelope) {

        return envelope;

    }


}
