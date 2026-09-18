package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("courtlist")
public interface QueryApiCourtlistResource {
    @GET
    @Produces("application/vnd.progression.search.court.list+json")
    Response getCourtlist(@QueryParam("courtCentreId") String courtCentreId,
                          @QueryParam("courtRoomId") String courtRoomId,
                          @QueryParam("listId") String listId,
                          @QueryParam("startDate") String startDate,
                          @QueryParam("endDate") String endDate,
                          @QueryParam("restricted") boolean restricted,
                          @HeaderParam(HeaderConstants.USER_ID) UUID userId);

    @GET
    @Produces("application/vnd.progression.search.prison.court.list+json")
    Response getPrisonCourtlist(@QueryParam("courtCentreId") String courtCentreId,
                          @QueryParam("courtRoomId") String courtRoomId,
                          @QueryParam("startDate") String startDate,
                          @QueryParam("endDate") String endDate,
                          @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
