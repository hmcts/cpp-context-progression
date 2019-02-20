package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class NowsDocumentApi {
    @Inject
    private Requester requester;

    @Handles("progression.query.material-nows-content")
    public JsonEnvelope getCaseDocumentDetails(final JsonEnvelope envelope) {

        return envelope;

    }


}
