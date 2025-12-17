package uk.gov.justice.api.resource;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
