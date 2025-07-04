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

    @Handles("progression.query.judicial-child-results")
    public JsonEnvelope getJudicialChildResults(final JsonEnvelope query) {
        return judicialResultQueryView.getJudicialChildResults(query);
    }

}
