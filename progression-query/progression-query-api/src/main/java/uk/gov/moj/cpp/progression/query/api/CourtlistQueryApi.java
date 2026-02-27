package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CourtlistQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtlistQueryApi {

    @Inject
    private CourtlistQueryView courtlistQueryView;

    @Handles("progression.search.court.list")
    public JsonEnvelope searchCourtlist(final JsonEnvelope query) {
        return courtlistQueryView.searchCourtlist(query);
    }

    @Handles("progression.search.prison.court.list")
    public JsonEnvelope searchPrisonCourtlist(final JsonEnvelope query) {
        return courtlistQueryView.searchPrisonCourtlist(query);
    }

    @Handles("progression.search.court.list.data")
    public JsonEnvelope searchCourtlistData(final JsonEnvelope query) {
        return courtlistQueryView.searchCourtlist(query);
    }

    @Handles("progression.search.prison.court.list.data")
    public JsonEnvelope searchPrisonCourtlistData(final JsonEnvelope query) {
        return courtlistQueryView.searchPrisonCourtlist(query);
    }
}
