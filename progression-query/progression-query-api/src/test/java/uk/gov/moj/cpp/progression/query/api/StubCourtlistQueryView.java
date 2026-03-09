package uk.gov.moj.cpp.progression.query.api;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CourtlistQueryView;

/**
 * Test stub for CourtlistQueryView. Used instead of mocking the concrete class
 * so tests run reliably on Java 17 without needing to mock complex bytecode.
 */
class StubCourtlistQueryView extends CourtlistQueryView {

    private JsonEnvelope searchCourtlistResponse;
    private JsonEnvelope searchPrisonCourtlistResponse;

    void setSearchCourtlistResponse(JsonEnvelope response) {
        this.searchCourtlistResponse = response;
    }

    void setSearchPrisonCourtlistResponse(JsonEnvelope response) {
        this.searchPrisonCourtlistResponse = response;
    }

    @Override
    public JsonEnvelope searchCourtlist(JsonEnvelope query) {
        return searchCourtlistResponse;
    }

    @Override
    public JsonEnvelope searchPrisonCourtlist(JsonEnvelope query) {
        return searchPrisonCourtlistResponse;
    }
}
