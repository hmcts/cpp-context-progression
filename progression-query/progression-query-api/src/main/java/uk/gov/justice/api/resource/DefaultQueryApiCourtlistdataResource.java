package uk.gov.justice.api.resource;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.moj.cpp.progression.query.api.service.CourtlistQueryService;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Returns court list data as JSON. Same parameters and access control as /courtlist,
 * but no PDF/Word generation and no pubhub. Shares logic with DefaultQueryApiCourtlistResource
 * via CourtlistQueryService.
 */
@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiCourtlistdataResource implements QueryApiCourtlistdataResource {

    private static final String PRISON_COURT_LIST = "PRISON";
    private static final String COURT_LIST_DATA_QUERY_NAME = "progression.search.court.list.data";
    private static final String PRISON_COURT_LIST_DATA_QUERY_NAME = "progression.search.prison.court.list.data";

    @Inject
    private CourtlistQueryService courtlistQueryService;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Override
    public Response getCourtlistdata(final String courtCentreId, final String courtRoomId, final String listId,
                                     final String startDate, final String endDate, final boolean restricted, final UUID userId) {
        if (PRISON_COURT_LIST.equals(listId)) {
            return status(FORBIDDEN).build();
        }
        return getCourtlistdataInternal(courtCentreId, courtRoomId, listId, startDate, endDate, restricted, userId, COURT_LIST_DATA_QUERY_NAME);
    }

    @Override
    public Response getPrisonCourtlistdata(final String courtCentreId, final String courtRoomId,
                                          final String startDate, final String endDate, final UUID userId) {
        return getCourtlistdataInternal(courtCentreId, courtRoomId, PRISON_COURT_LIST, startDate, endDate, false, userId, PRISON_COURT_LIST_DATA_QUERY_NAME);
    }

    private Response getCourtlistdataInternal(final String courtCentreId, final String courtRoomId, final String listId,
                                             final String startDate, final String endDate, final boolean restricted,
                                             final UUID userId, final String courtListAction) {
        final JsonEnvelope queryEnvelope = courtlistQueryService.buildCourtlistQueryEnvelope(
                courtCentreId, courtRoomId, listId, startDate, endDate, restricted, userId, courtListAction);
        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(queryEnvelope)).get();

        final JsonObject enrichedPayload = courtlistQueryService.buildEnrichedPayload(document);

        return ok(enrichedPayload).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
