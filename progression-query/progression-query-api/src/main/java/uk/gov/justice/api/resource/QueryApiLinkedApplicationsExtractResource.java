package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

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
