package uk.gov.justice.api.resource;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@FunctionalInterface
@Path("cases/{caseId}/casedocuments")
public interface UploadCaseDocumentsResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadCaseDocument(
                    @MultipartForm final MultipartFormDataInput multipartFormDataInput,
                    @HeaderParam(value = "CJSCPPUID") @DefaultValue("unknown") String userId,
                    @HeaderParam(value = "CPPSID") @DefaultValue("unknown") String session,
                    @HeaderParam(value = "CPPCLIENTCORRELATIONID") @DefaultValue("unknown") String correlationId,
                    @PathParam("caseId") String caseId) throws IOException;

}
