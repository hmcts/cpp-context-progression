package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtlistQueryApi {

    @Inject
    private Requester requester;

    @Handles("progression.search.court.list")
    public JsonEnvelope searchCourtlist(final JsonEnvelope query) {
        return requester.request(query);
    }
}
