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

@Path("material/{materialId}/content")
public interface QueryApiMaterialMaterialIdContentResource {
    @GET
    @Produces("application/vnd.progression.query.material-content+json")
    Response getMaterialByMaterialIdContent(
            @PathParam("materialId") String materialId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );

    @GET
    @Produces("application/vnd.progression.query.material-content-for-defence+json")
    Response getMaterialForDefenceByMaterialIdContent(
            @PathParam("materialId") String materialId,
            @QueryParam("defendantId") String defendantId,
            @QueryParam("applicationId") String applicationId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );

    @GET
    @Produces("application/vnd.progression.query.material-content-for-prosecution+json")
    Response getMaterialForProsecutionByMaterialIdContent(
            @PathParam("materialId") String materialId,
            @QueryParam("caseId") String caseId,
            @QueryParam("applicationId") String applicationId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
