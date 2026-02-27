package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("courtlistdata")
public interface QueryApiCourtlistdataResource {
    @GET
    @Produces("application/vnd.progression.search.court.list.data+json")
    Response getCourtlistdata(@QueryParam("courtCentreId") String courtCentreId,
                              @QueryParam("courtRoomId") String courtRoomId,
                              @QueryParam("listId") String listId,
                              @QueryParam("startDate") String startDate,
                              @QueryParam("endDate") String endDate,
                              @QueryParam("restricted") boolean restricted,
                              @HeaderParam(HeaderConstants.USER_ID) UUID userId);

    @GET
    @Produces("application/vnd.progression.search.prison.court.list.data+json")
    Response getPrisonCourtlistdata(@QueryParam("courtCentreId") String courtCentreId,
                                    @QueryParam("courtRoomId") String courtRoomId,
                                    @QueryParam("startDate") String startDate,
                                    @QueryParam("endDate") String endDate,
                                    @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
