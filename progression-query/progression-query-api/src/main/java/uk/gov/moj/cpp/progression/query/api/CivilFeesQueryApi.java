package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CivilFeesQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CivilFeesQueryApi {

    @Inject
    private CivilFeesQueryView civilFeesQueryView;

    @Handles("progression.query.civil-fee-details")
    public JsonEnvelope getCivilFees(final JsonEnvelope query) {
        return civilFeesQueryView.getCivilFees(query);
    }
}
