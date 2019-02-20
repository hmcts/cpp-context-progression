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

@Path("prosecutioncases/{caseId}/defendants/{defendantId}/extract/{template}")
public interface QueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource {
    @GET
    @Produces("application/vnd.progression.query.court-extract+json")
    Response getCourtExtractByCaseIdContent(
            @PathParam("caseId") String caseId,
            @PathParam("defendantId") String defendantId,
            @PathParam("template") String template,
            @QueryParam("hearingIds") String hearingIds,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId
    );
}
