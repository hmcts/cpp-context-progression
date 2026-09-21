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

@Path("applications/{applicationId}/defendants/{defendantId}/extract")
public interface QueryApiLinkedApplicationsExtractResource {
    @GET
    @Produces("application/vnd.progression.query.linked-application-extract+json")
    Response getApplicationsByApplicationIdDefendantsByDefendantIdExtract(
            @PathParam("applicationId") String applicationId,
            @PathParam("defendantId") String defendantId,
            @QueryParam("hearingIds") String hearingIds,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
