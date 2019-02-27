package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("material/{materialId}/content")
public interface QueryApiMaterialMaterialIdContentResource {
    @GET
    @Produces("application/vnd.progression.query.material-content+json")
    Response getMaterialByMaterialIdContent(
            @PathParam("materialId") String materialId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
