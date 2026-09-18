package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("material/nows/{materialId}/content")
public interface QueryApiMaterialNowsMaterialIdContentResource {
    @GET
    @Produces("application/vnd.progression.query.material-nows-content+json")
    Response getMaterialNowsByMaterialIdContent(@PathParam("materialId") String materialId,
                                                @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
