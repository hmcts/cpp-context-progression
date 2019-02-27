package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("material/nows/{materialId}/content")
public interface QueryApiMaterialNowsMaterialIdContentResource {
    @GET
    @Produces("application/vnd.progression.query.material-nows-content+json")
    Response getMaterialNowsByMaterialIdContent(@PathParam("materialId") String materialId,
                                                @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
