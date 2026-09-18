package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("prosecutioncases/{caseId}/defendants/{defendantId}/ejectcase")
public interface QueryApiProsecutioncasesCaseIdDefendantsDefendantIdEjectcaseResource {
    @GET
    @Produces("application/vnd.progression.query.eject-case+json")
    Response getProsecutioncasesByCaseIdDefendantsByDefendantIdEjectcase(
            @PathParam("caseId") String caseId,
            @PathParam("defendantId") String defendantId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
