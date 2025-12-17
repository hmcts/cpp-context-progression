package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.OpaNoticeQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class OpaNoticeQueryApi {

    @Inject
    private OpaNoticeQueryView opaNoticeQueryView;

    @Handles("progression.query.public-list-opa-notices")
    public JsonEnvelope getPublicListOpaNotices(final JsonEnvelope query) {
        return opaNoticeQueryView.getPublicListOpaNoticesView(query);
    }

    @Handles("progression.query.press-list-opa-notices")
    public JsonEnvelope getPressListOpaNotices(final JsonEnvelope query) {
        return opaNoticeQueryView.getPressListOpaNoticesView(query);
    }

    @Handles("progression.query.result-list-opa-notices")
    public JsonEnvelope getResultListOpaNotices(final JsonEnvelope query) {
        return opaNoticeQueryView.getResultListOpaNoticesView(query);
    }
}
