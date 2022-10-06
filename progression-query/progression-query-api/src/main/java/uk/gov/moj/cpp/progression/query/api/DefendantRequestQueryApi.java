package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.DefendantPartialMatchQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class DefendantRequestQueryApi {

    @Inject
    private DefendantPartialMatchQueryView defendantPartialMatchQueryView;

    @Handles("progression.query.defendant-request")
    public JsonEnvelope getDefendantRequest(final JsonEnvelope query) {
        return defendantPartialMatchQueryView.getDefendantPartialMatches(query);
    }
}
