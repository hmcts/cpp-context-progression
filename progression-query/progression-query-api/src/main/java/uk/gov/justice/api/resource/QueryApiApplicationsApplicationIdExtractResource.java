package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("applications/{applicationId}/extract")
public interface QueryApiApplicationsApplicationIdExtractResource {
    @GET
    @Produces("application/vnd.progression.query.court-extract-application+json")
    Response getApplicationExtractByApplicationIdContent(
            @PathParam("applicationId") String applicationId,
            @QueryParam("hearingIds") String hearingIds,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
