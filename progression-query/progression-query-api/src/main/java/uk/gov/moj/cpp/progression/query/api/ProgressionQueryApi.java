package uk.gov.moj.cpp.progression.query.api;



import javax.inject.Inject;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.QUERY_API)
public class ProgressionQueryApi {

    @Inject
    private Requester requester;

    @Handles("progression.query.caseprogressiondetail")
    public JsonEnvelope getCaseprogressiondetail(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("progression.query.timeline")
    public JsonEnvelope getTimeline(final JsonEnvelope query) {
        return requester.request(query);
    }
    
    @Handles("progression.query.indicatestatementsdetails")
    public JsonEnvelope getIndicatestatementsdetails(final JsonEnvelope query) {
        return requester.request(query);
    }
    
    @Handles("progression.query.indicatestatementsdetail")
    public JsonEnvelope getIndicatestatementsdetail(final JsonEnvelope query) {
        return requester.request(query);
    }
    
    @Handles("progression.query.cases")
    public JsonEnvelope getCases(final JsonEnvelope query) {
        return requester.request(query);
    }
}
