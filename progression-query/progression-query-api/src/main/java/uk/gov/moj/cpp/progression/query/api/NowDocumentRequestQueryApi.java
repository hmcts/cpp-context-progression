package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.NowDocumentRequestQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class NowDocumentRequestQueryApi {
    @Inject
    private NowDocumentRequestQueryView nowDocumentRequestQueryView;

    @Handles("progression.query.now-document-requests-by-request-id")
    public JsonEnvelope getNowDocumentRequests(final JsonEnvelope query) {
        return nowDocumentRequestQueryView.getNowDocumentRequestsByRequestId(query);
    }

    @Handles("progression.query.now-document-request-by-hearing")
    public JsonEnvelope getNowDocumentRequestsByHearing(final JsonEnvelope query) {
        return nowDocumentRequestQueryView.getNowDocumentRequestByHearing(query);
    }
}
