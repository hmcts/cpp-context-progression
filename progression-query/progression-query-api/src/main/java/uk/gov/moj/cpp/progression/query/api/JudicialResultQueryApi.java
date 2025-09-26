package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.JudicialResultQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class JudicialResultQueryApi {

    @Inject
    private JudicialResultQueryView judicialResultQueryView;

    /**
     * This Api specifically is using by a BDF for CCT-2065 in order to update
     * unpaidWork and expiry date correctly
     * @param query
     * @return
     */
    @Handles("progression.query.judicial-child-results")
    public JsonEnvelope getJudicialChildResults(final JsonEnvelope query) {
        return judicialResultQueryView.getJudicialChildResults(query);
    }

    /**
     * This Api specifically is using by a BDF for CCT-2065 in order to update
     * unpaidWork and expiry date correctly
     * @param query
     * @return
     */
    @Handles("progression.query.judicial-child-results-v2")
    public JsonEnvelope getJudicialChildResultsV2(final JsonEnvelope query) {
        return judicialResultQueryView.getJudicialChildResultsV2(query);
    }

}
